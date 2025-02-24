package com.example.chatapp.utils

import android.util.Log
import com.example.chatapp.models.Chat
import com.example.chatapp.models.ChatMember
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.models.FriendRequest
import com.example.chatapp.models.Notification
import com.example.chatapp.models.RecentChat
import com.example.chatapp.models.RequestStatus
import com.example.chatapp.models.User
import com.example.chatapp.models.UserWithFriendStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class FirebaseHelper {
    //realtime database
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val friendsRef: DatabaseReference = database.getReference("friends")
    private val usersRef: DatabaseReference = database.getReference("users")
    private val chatsRef: DatabaseReference = database.getReference("chats")
    private val chatMemberRef : DatabaseReference = database.getReference("chat_member")
    private val chatMessageRef : DatabaseReference = database.getReference("chat_messages")
    private val friendRequest: DatabaseReference = database.getReference("friend_requests")
    private val notifications: DatabaseReference = database.getReference("notifications")
    private val recentChat: DatabaseReference = database.getReference("recent_chat")


    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getUserById(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit) {
        usersRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    onSuccess(user)
                } else {
                    onFailure("User not found")
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to fetch user")
            }
    }


    fun getActiveFriends(callback: (List<User>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Lấy danh sách ID bạn bè của người dùng hiện tại
        friendsRef.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(friendsSnapshot: DataSnapshot) {
                val friendIds = friendsSnapshot.children.mapNotNull { it.key } // Lấy danh sách ID bạn bè

                // Lắng nghe sự thay đổi ở bảng "users" == bổ sung nếu hủy kết bạn thì không hiển thị trên danh sách nữa
                usersRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val activeFriends = snapshot.children.mapNotNull {
                            val user = it.getValue(User::class.java)
                            if (user != null && friendIds.contains(user.id)) {
                                User(user.id, user.name, user.email, user.profileImage, user.status, user.lastSeen)
                            } else null
                        }.sortedByDescending { it.status } // Ưu tiên hiển thị người đang online trước

                        callback(activeFriends) // Gửi danh sách bạn bè cập nhật về UI
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(emptyList())
                    }
                })


            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun countFriends(callback: (Int) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        friendsRef.child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendCount = snapshot.childrenCount.toInt() // Lấy số lượng bạn bè
                callback(friendCount) // Trả về kết quả
            }

            override fun onCancelled(error: DatabaseError) {
                callback(0) // Nếu có lỗi, trả về 0
            }
        })
    }


    fun getRecentChats(callback: (List<RecentChat>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        recentChat.child(currentUserId).orderByChild("formattedTimestamp").limitToLast(20)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val recentChats = snapshot.children.mapNotNull { it.getValue(RecentChat::class.java) }

                    if (recentChats.isEmpty()) {
                        callback(emptyList())
                        return
                    }

                    // Lấy danh sách các userId cần cập nhật thông tin
                    val userIds = recentChats.map { it.otherUserId }.toSet()
                    val updatedChats = mutableListOf<RecentChat>()

                    // Đếm số lần userName được cập nhật để chỉ gọi callback khi hoàn tất
                    var usersProcessed = 0

                    for (userId in userIds) {
                        usersRef.child(userId).addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {

                                val userName = userSnapshot.child("name").getValue(String::class.java) ?: "Người dùng"
                                val userImage = userSnapshot.child("profileImage").getValue(String::class.java) ?: ""
                                val status = userSnapshot.child("status").getValue(Boolean::class.java) ?: false

                                recentChats.forEach { recentChat ->
                                    if(recentChat.group){
                                        if (recentChat.otherUserId == userId) {
                                            updatedChats.add(
                                                recentChat.copy(otherUserName = recentChat.otherUserName, otherUserImage = recentChat.otherUserImage, isStatus = true)
                                            )
                                        }
                                    }
                                    else{
                                        if (recentChat.otherUserId == userId) {
                                            updatedChats.add(
                                                recentChat.copy(otherUserName = userName, otherUserImage = userImage, isStatus = status)
                                            )
                                        }
                                    }

                                }

                                usersProcessed++
                                if (usersProcessed == userIds.size) {
                                    callback(updatedChats.sortedByDescending { it.formattedTimestamp }) // Chỉ gọi callback khi tất cả userName đã cập nhật xong
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                usersProcessed++
                                if (usersProcessed == userIds.size) {
                                    callback(updatedChats)
                                }
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }


    fun listenForFriendRequests(callback: (List<Pair<FriendRequest, User?>>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        friendRequest.child(currentUserId).orderByChild("status").equalTo(RequestStatus.PENDING.name)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requestList = mutableListOf<Pair<FriendRequest, User?>>()
                    val friendRequests = snapshot.children.mapNotNull { it.getValue(FriendRequest::class.java) }

                    if (friendRequests.isEmpty()) {
                        callback(emptyList())
                        return
                    }

                    var processedCount = 0
                    for (request in friendRequests) {
                        usersRef.child(request.senderId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val user = userSnapshot.getValue(User::class.java)
                                requestList.add(Pair(request, user))

                                processedCount++
                                if (processedCount == friendRequests.size) {
                                    callback(requestList)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                processedCount++
                                if (processedCount == friendRequests.size) {
                                    callback(requestList)
                                }
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList()) // Trả về danh sách rỗng nếu có lỗi
                }
            })
    }


    fun getUsersWithFriendStatus(callback: (List<UserWithFriendStatus>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val friendsRef = FirebaseDatabase.getInstance().getReference("friends").child(currentUserId)

        friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(friendsSnapshot: DataSnapshot) {
                val usersList = mutableListOf<UserWithFriendStatus>()
                val friendIds = mutableSetOf<String>()
                var processedCount = 0

                // Lấy danh sách ID bạn bè
                for (friend in friendsSnapshot.children) {
                    friend.key?.let { friendIds.add(it) }
                }

                if (friendIds.isEmpty()) {
                    callback(emptyList())
                    return
                }

                for (friendId in friendIds) {
                    usersRef.child(friendId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                usersList.add(UserWithFriendStatus(user.id, user.name, user.email, true, user.profileImage))
                            }
                            processedCount++
                            if (processedCount == friendIds.size) {
                                callback(usersList) // Gửi danh sách cập nhật khi lấy đủ dữ liệu
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            processedCount++
                            if (processedCount == friendIds.size) {
                                callback(usersList) // Gửi danh sách ngay cả khi có lỗi
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList()) // Trả về danh sách rỗng nếu có lỗi
            }
        })
    }




    fun getUsers(callback: (List<UserWithFriendStatus>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val friendsRef = FirebaseDatabase.getInstance().getReference("friends").child(currentUserId)


        usersRef.get().addOnSuccessListener { usersSnapshot ->
            friendsRef.get().addOnSuccessListener { friendsSnapshot ->
                val usersList = mutableListOf<UserWithFriendStatus>()
                val friendsSet = mutableSetOf<String>()

                // Lấy danh sách bạn bè của currentUser
                for (friend in friendsSnapshot.children) {
                    friendsSet.add(friend.key ?: "")
                }

                // Lấy danh sách tất cả user
                for (user in usersSnapshot.children) {
                    val userId = user.key ?: continue
                    if (userId == currentUserId) continue

                    val userName = user.child("name").getValue(String::class.java) ?: "Unknown"
                    val email = user.child("email").getValue(String::class.java) ?: "Unknown"
                    val profileImage = user.child("profileImage").getValue(String::class.java) ?: "Unknown"
                    val isFriend = friendsSet.contains(userId)

                    usersList.add(UserWithFriendStatus(userId, userName, email, isFriend, profileImage))
                }

                callback(usersList) // Trả về danh sách user
            }.addOnFailureListener {
                callback(emptyList()) // Nếu lỗi, trả về danh sách rỗng
            }
        }.addOnFailureListener {
            callback(emptyList()) // Nếu lỗi, trả về danh sách rỗng
        }
    }


    fun sendFriendRequest(receiverId: String?, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (receiverId == null) return
        val timestamp = System.currentTimeMillis()

        // Kiểm tra xem receiverId đã gửi lời mời kết bạn đến currentUserId chưa
        friendRequest.child(currentUserId).child(receiverId).get()
            .addOnSuccessListener { snapshot ->
                val existingRequest = snapshot.child("status").getValue(String::class.java)

                if (existingRequest == RequestStatus.PENDING.name) {
                    // Nếu receiverId đã gửi lời mời kết bạn trước đó, thực hiện kết bạn
                    val friendsUpdate = mapOf(
                        "friends/${currentUserId}/${receiverId}" to true,
                        "friends/${receiverId}/${currentUserId}" to true
                    )

                    // Cập nhật danh sách bạn bè
                    database.reference.updateChildren(friendsUpdate)
                        .addOnSuccessListener {
                            // Xóa lời mời kết bạn
                            friendRequest.child(currentUserId).child(receiverId).removeValue()
                            friendRequest.child(receiverId).child(currentUserId).removeValue()

                            callback(true, "Hai bạn đã trở thành bạn bè!")
                        }
                        .addOnFailureListener {
                            callback(false, "Kết bạn thất bại")
                        }
                } else {
                    // Nếu chưa có yêu cầu từ receiverId, tạo yêu cầu kết bạn mới
                    val request = mapOf(
                        "senderId" to currentUserId,
                        "receiverId" to receiverId,
                        "status" to RequestStatus.PENDING.name,
                        "timestamp" to timestamp
                    )

                    // Lưu vào friend_requests
                    friendRequest.child(receiverId).child(currentUserId).setValue(request)
                        .addOnSuccessListener {
                            // Gửi thông báo
                            sendNotification(receiverId, "Bạn nhận được lời mời kết bạn")
                            callback(true, "Lời mời kết bạn đã được gửi")
                        }
                        .addOnFailureListener {
                            callback(false, "Gửi lời mời kết bạn thất bại")
                        }
                }
            }
            .addOnFailureListener {
                callback(false, "Lỗi khi kiểm tra yêu cầu kết bạn")
            }
    }


    private fun sendNotification(receiverId: String, message: String, type:String? = "") {
        val notificationId = notifications.child(receiverId).push().key ?: return
        val timestamp = System.currentTimeMillis()

        val notificationData = mapOf(
            "senderId" to auth.currentUser?.uid,
            "message" to message,
            "timestamp" to timestamp,
            "read" to false
        )

        notifications.child(receiverId).child(notificationId).setValue(notificationData)
    }

    fun listenForNotifications(callback: (Notification) -> Unit) {
        val userIdCurrent = auth.currentUser?.uid ?: return

        notifications.child(userIdCurrent).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notification = snapshot.getValue(Notification::class.java) ?: return
                val notificationKey = snapshot.key ?: return

                // Nếu thông báo đã đọc thì bỏ qua
                if (notification.read) return

                // Cập nhật read = true trên Firebase
                notifications.child(userIdCurrent).child(notificationKey).child("read").setValue(true)

                // Gọi callback với thông báo chưa đọc
                callback(notification)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun cancelFriendship(friendId: String?, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        if(friendId == null)return
        val friendRef = database.getReference("friends").child(currentUserId).child(friendId)
        val reverseFriendRef = database.getReference("friends").child(friendId).child(currentUserId)

        friendRef.removeValue()
            .addOnSuccessListener {
                reverseFriendRef.removeValue()
                    .addOnSuccessListener {
                        callback(true, "Bạn đã hủy kết bạn")
                    }
                    .addOnFailureListener {
                        callback(false, "Lỗi khi hủy kết bạn")
                    }
            }
            .addOnFailureListener {
                callback(false, "Lỗi khi hủy kết bạn")
            }
    }

    fun acceptFriendRequest(senderId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        val currentUserRef = database.getReference("friends").child(currentUserId).child(senderId)
        val senderUserRef = database.getReference("friends").child(senderId).child(currentUserId)
        val requestRef = database.getReference("friend_requests").child(currentUserId).child(senderId)
        val senderRequestRef = database.getReference("friend_requests").child(senderId).child(currentUserId)

        // Cập nhật trạng thái trong bảng `friend_requests` thành "ACCEPTED"
        val updateStatus = mapOf("status" to "ACCEPTED")

        requestRef.updateChildren(updateStatus)
            .addOnSuccessListener {
                senderRequestRef.updateChildren(updateStatus)
                    .addOnSuccessListener {
                        // Thêm vào danh sách bạn bè
                        currentUserRef.setValue(true)
                            .addOnSuccessListener {
                                senderUserRef.setValue(true)
                                    .addOnSuccessListener {
                                        // Xóa lời mời kết bạn sau khi đã đồng ý
                                        requestRef.removeValue()
                                        senderRequestRef.removeValue()

                                        sendNotification(senderId, "Lời mời kết bạn đã được chấp nhận!")

                                        checkExistingChat(senderId, currentUserId) { chatId ->
                                            if (chatId == null) {
                                                createNewChat(senderId, currentUserId) { newChatId -> }
                                            }
                                        }
                                        callback(true, "Bạn đã chấp nhận kết bạn")
                                    }
                                    .addOnFailureListener { callback(false, "Lỗi khi lưu bạn bè") }
                            }
                            .addOnFailureListener { callback(false, "Lỗi khi lưu bạn bè") }
                    }
                    .addOnFailureListener { callback(false, "Lỗi khi cập nhật trạng thái friend_request") }
            }
            .addOnFailureListener { callback(false, "Lỗi khi cập nhật trạng thái friend_request") }
    }

    private fun checkExistingChat(userId1: String, userId2: String, callback: (String?) -> Unit) {
        val chatQuery = chatMemberRef.orderByChild("userId").equalTo(userId1)

        chatQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user1ChatIds = mutableSetOf<String>()

                // Lưu danh sách chatId của userId1
                for (chat in snapshot.children) {
                    val chatMember = chat.getValue(ChatMember::class.java)
                    chatMember?.let { user1ChatIds.add(it.chatId) }
                }

                // Truy vấn danh sách chat của userId2
                val receiverQuery = chatMemberRef.orderByChild("userId").equalTo(userId2)
                receiverQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(receiverSnapshot: DataSnapshot) {
                        for (chat in receiverSnapshot.children) {
                            val chatMember = chat.getValue(ChatMember::class.java)
                            if (chatMember != null && user1ChatIds.contains(chatMember.chatId)) {
                                callback(chatMember.chatId)
                                return
                            }
                        }
                        callback(null)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseHelper", "Lỗi khi truy vấn receiver: ${error.message}")
                        callback(null)
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseHelper", "Lỗi khi truy vấn sender: ${error.message}")
                callback(null)
            }
        })
    }


    private fun createNewChat(userId1: String, userId2: String, callback: (String) -> Unit) {
        val chatId = chatsRef.push().key!!
        val newChat = Chat(chatId,"", Date(), "", "", "" ,false)

        chatsRef.child(chatId).setValue(newChat).addOnSuccessListener {
            val senderMember = ChatMember(
                chatMemberId = chatMemberRef.push().key!!,
                chatId = chatId,
                userId = userId1
            )
            chatMemberRef.child(senderMember.chatMemberId).setValue(senderMember)

            val receiverMember = ChatMember(
                chatMemberId = chatMemberRef.push().key!!,
                chatId = chatId,
                userId = userId2
            )
            chatMemberRef.child(receiverMember.chatMemberId).setValue(receiverMember)
                .addOnSuccessListener { callback(chatId) }
                .addOnFailureListener { Log.e("FirebaseHelper", "Lỗi khi thêm receiver vào nhóm chat") }
        }.addOnFailureListener {
            Log.e("FirebaseHelper", "Lỗi khi tạo nhóm chat")
        }
    }

    fun createGroupChat(users: List<UserWithFriendStatus>, callback: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (users.isEmpty()) {
            Log.e("FirebaseHelper", "Không có thành viên nào để tạo chat")
            callback(false)
        }
        val chatId = chatsRef.push().key!!
        val newChat = Chat(chatId,"Nhóm chat", Date(),currentUserId, currentUserId, "",true)

        chatsRef.child(chatId).setValue(newChat).addOnSuccessListener {
            val listUser = mutableListOf<UserWithFriendStatus>();

            listUser.add(UserWithFriendStatus(userId = currentUserId, "Current", "test", true, ""))
            for(user in users) {
                listUser.add(user)
            }
            for (user in listUser) {
                val chatMemberId = chatMemberRef.push().key!!
                val chatMember = ChatMember(
                    chatMemberId = chatMemberId,
                    chatId = chatId,
                    userId = user.userId
                )
                chatMemberRef.child(chatMemberId).setValue(chatMember)

                if(user.userId == currentUserId){
                    val receiverRecentChat = mapOf(
                        "chatId" to chatId,
                        "otherUserId" to user.userId,
                        "otherUserName" to "Nhóm chat",
                        "lastMessage" to "Bạn vừa tạo một nhóm chat",
                        "formattedTimestamp" to System.currentTimeMillis(),
                        "group" to true,
                        "otherUserImage" to "https://res.cloudinary.com/db4jiiw1w/image/upload/v1740040957/group_chat_r8gdo9.jpg"
                    )
                    recentChat.child(user.userId).child(chatId).setValue(receiverRecentChat)
                }else{
                    val receiverRecentChat = mapOf(
                        "chatId" to chatId,
                        "otherUserId" to user.userId,
                        "otherUserName" to "Nhóm chat",
                        "lastMessage" to "Bạn vừa được add vào một nhóm chat",
                        "formattedTimestamp" to System.currentTimeMillis(),
                        "group" to true,
                        "otherUserImage" to "https://res.cloudinary.com/db4jiiw1w/image/upload/v1740040957/group_chat_r8gdo9.jpg"
                    )
                    recentChat.child(user.userId).child(chatId).setValue(receiverRecentChat)
                }


            }
            callback(true)

        }.addOnFailureListener { exception ->
            callback(false)
            Log.e("FirebaseHelper", "Lỗi khi tạo nhóm chat: ${exception.message}")
        }
    }

    fun addGroupChat(users: List<UserWithFriendStatus>,chatId: String, callback: (Boolean, String) -> Unit) {
        if (users.isEmpty()) {
            Log.e("FirebaseHelper", "Không có thành viên nào để tạo chat")
            callback(false, "")
        }

        chatMemberRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                callback(false,"")
                return@addOnSuccessListener
            }
            val existingUserIds = snapshot.children
                .mapNotNull { it.getValue(ChatMember::class.java) }
                .filter { it.chatId == chatId }
                .map { it.userId }

            val existingUserNames = users.filter { it.userId in existingUserIds }
                .joinToString(", ") { it.userName }

            val newUsers = users.filter { it.userId !in existingUserIds }

            if (newUsers.isEmpty()) {
                Log.e("FirebaseHelper", "Tất cả người dùng đã có trong nhóm")
                callback(false, existingUserNames)
                return@addOnSuccessListener
            }
            chatsRef.child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatInfo = snapshot.getValue(Chat::class.java)
                    if(chatInfo != null){
                        for (user in newUsers) {
                            val chatMemberId = chatMemberRef.push().key!!
                            val chatMember = ChatMember(
                                chatMemberId = chatMemberId,
                                chatId = chatId,
                                userId = user.userId
                            )
                            chatMemberRef.child(chatMemberId).setValue(chatMember)

                            val receiverRecentChat = mapOf(
                                "chatId" to chatId,
                                "otherUserId" to user.userId,
                                "otherUserName" to chatInfo.chatName,
                                "lastMessage" to "Bạn vừa được add vào một nhóm chat",
                                "formattedTimestamp" to System.currentTimeMillis(),
                                "group" to true,
                                "otherUserImage" to "https://res.cloudinary.com/db4jiiw1w/image/upload/v1740040957/group_chat_r8gdo9.jpg"
                            )
                            recentChat.child(user.userId).child(chatId).setValue(receiverRecentChat)
                        }
                        for(oldUser in existingUserIds){
                            val receiverRecentChat = mapOf(
                                "chatId" to chatId,
                                "otherUserId" to oldUser,
                                "otherUserName" to chatInfo.chatName,
                                "lastMessage" to "Nhóm chat có ${newUsers.size} thành viên mới",
                                "formattedTimestamp" to System.currentTimeMillis(),
                                "group" to true,
                                "otherUserImage" to "https://res.cloudinary.com/db4jiiw1w/image/upload/v1740040957/group_chat_r8gdo9.jpg"
                            )
                            recentChat.child(oldUser).child(chatId).updateChildren(receiverRecentChat)
                        }
                        callback(true, "")
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, "")
                }
            })
        }.addOnFailureListener {
            Log.e("FirebaseHelper", "Lỗi khi kiểm tra thành viên nhóm: ${it.message}")
            callback(false, "")
        }
    }




    fun declineFriendRequest(senderId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return

        val requestRef = database.getReference("friend_requests").child(currentUserId).child(senderId)

        requestRef.removeValue()
            .addOnSuccessListener {
                //sendNotification(senderId, "Lời mời kết bạn đã bị từ chối!")
                callback(true, "Bạn đã từ chối lời mời kết bạn")
            }
            .addOnFailureListener { callback(false, "Lỗi khi từ chối lời mời") }
    }

    fun areFriends(userId1: String?, userId2: String?, callback: (Boolean) -> Unit) {
        if (userId1 == null || userId2 == null) return

        friendsRef.child(userId1).child(userId2)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists()) // Nếu tồn tại -> là bạn bè
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false) // Xảy ra lỗi, mặc định là không phải bạn bè
                }
            })
    }

    fun hasPendingFriendRequest(senderId: String?, receiverId: String?, callback: (Boolean) -> Unit) {
        if (senderId == null || receiverId == null) return

        friendRequest.child(receiverId).child(senderId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    callback(status == "PENDING") // Nếu status là "PENDING" -> có yêu cầu chờ duyệt
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false) // Lỗi khi đọc dữ liệu, mặc định không có yêu cầu
                }
            })
    }

    fun updateUserStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val statusMap = mapOf(
            "status" to isOnline,
            "lastSeen" to if (isOnline) null else System.currentTimeMillis()
        )

        usersRef.child(userId).updateChildren(statusMap)
    }

    fun sendMessage(senderId: String?, receiverId: String, messageText: String) {
        if (senderId == null) return

        val chatQuery = chatMemberRef
            .orderByChild("userId")
            .equalTo(senderId)

        chatQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val senderChatIds = mutableSetOf<String>()
                var chatId: String = ""

                // Lưu danh sách chatId của senderId
                for (chat in snapshot.children) {
                    val chatMember = chat.getValue(ChatMember::class.java)
                    if (chatMember != null) {
                        senderChatIds.add(chatMember.chatId)
                    }
                }

                // Kiểm tra xem có chatId nào mà cả senderId và receiverId cùng tham gia không
                val receiverQuery = chatMemberRef
                    .orderByChild("userId")
                    .equalTo(receiverId)

                receiverQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(receiverSnapshot: DataSnapshot) {
                        for (chat in receiverSnapshot.children) {
                            val chatMember = chat.getValue(ChatMember::class.java)
                            if (chatMember != null && senderChatIds.contains(chatMember.chatId)) {
                                chatId = chatMember.chatId
                                break
                            }
                        }



                        usersRef.child(receiverId).child("name").get()
                            .addOnSuccessListener { receiverSnapshot ->
                                val receiverName = receiverSnapshot.getValue(String::class.java) ?: "Người dùng"
                                val senderRecentChat = mapOf(
                                    "chatId" to chatId,
                                    "otherUserId" to receiverId,
                                    "otherUserName" to receiverName,
                                    "lastMessage" to messageText,
                                    "formattedTimestamp" to System.currentTimeMillis(),
                                    "group" to false
                                )

                                recentChat.child(senderId).child(chatId).get().addOnSuccessListener { snapshot ->
                                    if (!snapshot.exists()) {
                                        recentChat.child(senderId).child(chatId).setValue(senderRecentChat)
                                    } else {
                                        recentChat.child(senderId).child(chatId).updateChildren(senderRecentChat)
                                    }
                                }
                            }
                        // Lấy tên người gửi
                        usersRef.child(senderId).child("name").get()
                            .addOnSuccessListener { senderSnapshot ->
                                val senderName = senderSnapshot.getValue(String::class.java) ?: "Người dùng"
                                sendNotification(receiverId, "${senderName.toString()} đã nhắn cho bạn", "message")

                                // Cập nhật RecentChat cho người nhận
                                val receiverRecentChat = mapOf(
                                    "chatId" to chatId,
                                    "otherUserId" to senderId,
                                    "otherUserName" to senderName,
                                    "lastMessage" to messageText,
                                    "formattedTimestamp" to System.currentTimeMillis(),
                                    "group" to false
                                )

                                // Gửi tin nhắn
                                val messageId = chatMessageRef.push().key!!
                                val chatMessage = ChatMessage(
                                    chatMessageId = messageId,
                                    chatId = chatId,
                                    senderId = senderId,
                                    senderName = senderName,
                                    text = messageText,
                                    timestamp = System.currentTimeMillis(),
                                    senderAvatarUrl = ""
                                )
                                chatMessageRef.child(messageId).setValue(chatMessage)


                                // Cập nhật vào Firebase Realtime Database

                                recentChat.child(receiverId).child(chatId).get().addOnSuccessListener { snapshot ->
                                    if (!snapshot.exists()) {
                                        recentChat.child(receiverId).child(chatId).setValue(receiverRecentChat)
                                    } else {
                                        recentChat.child(receiverId).child(chatId).updateChildren(receiverRecentChat)
                                    }
                                }
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseHelper", "Lỗi khi truy vấn receiver: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseHelper", "Lỗi khi truy vấn sender: ${error.message}")
            }
        })
    }

    fun sendMessageGroup(senderId: String?, chatId: String, messageText: String) {
        if (senderId == null) return

        // Tạo tin nhắn mới
        val messageId = chatMessageRef.push().key!!
        usersRef.child(senderId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    val chatMessage = ChatMessage(
                        chatMessageId = messageId,
                        chatId = chatId,
                        senderId = senderId,
                        senderName = user.name,
                        text = messageText,
                        timestamp = System.currentTimeMillis(),
                        senderAvatarUrl = ""
                    )
                    // Lưu tin nhắn vào Firebase
                    chatMessageRef.child(messageId).setValue(chatMessage)
                }
            }


        chatMemberRef.orderByChild("chatId").equalTo(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val chatMember = child.getValue(ChatMember::class.java)

                    if(chatMember != null){
                        usersRef.child(chatMember.userId).child("name").get()
                            .addOnSuccessListener { receiverSnapshot ->
                                if(chatMember.userId != senderId){
                                    sendNotification(chatMember.userId, "Bạn nhận được tin nhắn từ nhóm chat", "message")
                                }

                                val senderRecentChat = mapOf(
                                    "chatId" to chatId,
                                    "otherUserId" to chatMember.userId,
                                    "otherUserName" to "Nhóm chat",
                                    "lastMessage" to messageText,
                                    "formattedTimestamp" to System.currentTimeMillis(),
                                    "group" to true,
                                    "isStatus" to true
                                )

                                // Cập nhật recent chat cho sender
                                recentChat.child(chatMember.userId).child(chatId).get().addOnSuccessListener { snapshot ->
                                    if (!snapshot.exists()) {
                                        recentChat.child(chatMember.userId).child(chatId).setValue(senderRecentChat)
                                    } else {
                                        recentChat.child(chatMember.userId).child(chatId).updateChildren(senderRecentChat)
                                    }
                                }
                            }
                    }

                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Lỗi truy vấn: ${error.message}")
            }
        })
    }


    fun updateMessageText(chatMessageId: String, newText: String, currentId: String?) {
        chatMessageRef.child(chatMessageId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null && chatMessage.senderId == currentId) {
                    chatMessageRef.child(chatMessageId).child("text").setValue(newText)
                        .addOnSuccessListener { println("Tin nhắn được cập nhật thành công.") }
                        .addOnFailureListener { println("Lỗi khi cập nhật tin nhắn: ${it.message}") }
                } else {
                    println("Không thể cập nhật tin nhắn của người khác.")
                }
            }
        }.addOnFailureListener {
            println("Lỗi khi lấy tin nhắn: ${it.message}")
        }
    }

    fun deleteMessage(chatMessageId: String, currentId: String?) {
        chatMessageRef.child(chatMessageId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null && chatMessage.senderId == currentId) {
                    chatMessageRef.child(chatMessageId).removeValue()
                        .addOnSuccessListener { println("Tin nhắn đã được xóa.") }
                        .addOnFailureListener { println("Lỗi khi xóa tin nhắn: ${it.message}") }
                } else {
                    println("Không thể xóa tin nhắn của người khác.")
                }
            }
        }.addOnFailureListener {
            println("Lỗi khi lấy tin nhắn: ${it.message}")
        }
    }

    fun listenForMessages(senderId: String?, receiverId: String,
                          onMessageReceived: (ChatMessage) -> Unit,
                          onUpdateMessage: (ChatMessage) -> Unit,
                          onDeleteMessage: (ChatMessage) -> Unit) {
        val senderChatIds = mutableSetOf<String>()

        // Lấy danh sách chatId của senderId
        val senderQuery = chatMemberRef.orderByChild("userId").equalTo(senderId)
        senderQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(senderSnapshot: DataSnapshot) {
                for (chat in senderSnapshot.children) {
                    val chatMember = chat.getValue(ChatMember::class.java)
                    if (chatMember != null) {
                        senderChatIds.add(chatMember.chatId)
                    }
                }

                if (senderChatIds.isEmpty()) {
                    Log.e("FirebaseHelper", "Người gửi chưa tham gia chat nào")
                    return
                }

                // Lấy danh sách chatId của receiverId
                val receiverQuery = chatMemberRef.orderByChild("userId").equalTo(receiverId)
                receiverQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(receiverSnapshot: DataSnapshot) {
                        var chatId: String? = null

                        for (chat in receiverSnapshot.children) {
                            val chatMember = chat.getValue(ChatMember::class.java)
                            if (chatMember != null && senderChatIds.contains(chatMember.chatId)) {
                                chatId = chatMember.chatId
                                break
                            }
                        }

                        if (chatId != null) {
                            // Kiểm tra số lượng thành viên trong chatId
                            chatMemberRef.orderByChild("chatId").equalTo(chatId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(chatMemberSnapshot: DataSnapshot) {
                                        if (chatMemberSnapshot.childrenCount == 2L) {
                                            // Chỉ lắng nghe tin nhắn nếu chat có đúng 2 user
                                            listenToChatMessages(chatId, onMessageReceived, onUpdateMessage, onDeleteMessage)
                                        } else {
                                            Log.e("FirebaseHelper", "ChatId $chatId có nhiều hơn 2 user, bỏ qua.")
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("FirebaseHelper", "Lỗi khi kiểm tra số lượng thành viên: ${error.message}")
                                    }
                                })
                        } else {
                            Log.e("FirebaseHelper", "Không tìm thấy chat chung giữa $senderId và $receiverId")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseHelper", "Lỗi khi truy vấn receiverId: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseHelper", "Lỗi khi truy vấn senderId: ${error.message}")
            }
        })
    }


    private fun listenToChatMessages(chatId: String,
                                     onMessageReceived: (ChatMessage) -> Unit,
                                     onUpdateMessage: (ChatMessage) -> Unit,
                                     onDeleteMessage: (ChatMessage) -> Unit) {
        chatMessageRef.orderByChild("chatId").equalTo(chatId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val chatMessage = snapshot.getValue(ChatMessage::class.java)
                    if (chatMessage != null) {
                        usersRef.child(chatMessage.senderId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val user = userSnapshot.getValue(User::class.java)
                                val senderImage = user?.profileImage ?: ""
                                val senderName = user?.name ?: ""
                                chatMessage.senderName = senderName
                                chatMessage.senderAvatarUrl = senderImage
                                onMessageReceived(chatMessage)
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseHelper", "Không thể lấy thông tin người dùng: ${it.message}")
                                onMessageReceived(chatMessage)
                            }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val updatedMessage = snapshot.getValue(ChatMessage::class.java)
                    if (updatedMessage != null) {
                        usersRef.child(updatedMessage.senderId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val user = userSnapshot.getValue(User::class.java)
                                val senderImage = user?.profileImage ?: ""
                                val senderName = user?.name ?: ""
                                updatedMessage.senderName = senderName
                                updatedMessage.senderAvatarUrl = senderImage
                                onUpdateMessage(updatedMessage)
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseHelper", "Không thể lấy thông tin người dùng: ${it.message}")
                                onUpdateMessage(updatedMessage)
                            }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val removedMessage = snapshot.getValue(ChatMessage::class.java)
                    if (removedMessage != null) {
                        onDeleteMessage(removedMessage) // Gửi tin nhắn đã bị xóa về ViewModel
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseHelper", "Lỗi khi lắng nghe tin nhắn mới: ${error.message}")
                }
            })
    }
//    fun listenForMessages(
//        senderId: String?,
//        receiverId: String,
//        onMessageReceived: (ChatMessage) -> Unit,
//        onUpdateMessage: (ChatMessage) -> Unit,
//        onDeleteMessage: (ChatMessage) -> Unit
//    ) {
//        if (senderId == null) {
//            Log.e("FirebaseHelper", "senderId không hợp lệ")
//            return
//        }
//
//        getChatIdBetweenUsers(senderId, receiverId) { chatId ->
//            if (chatId != null) {
//                listenToAllMessages(chatId, onMessageReceived, onUpdateMessage, onDeleteMessage)
//            } else {
//                Log.e("FirebaseHelper", "Không tìm thấy chatId giữa $senderId và $receiverId")
//            }
//        }
//    }

    // Lấy chatId giữa senderId và receiverId
//    private fun getChatIdBetweenUsers(senderId: String, receiverId: String, callback: (String?) -> Unit) {
//        val senderChatIds = mutableSetOf<String>()
//
//        // Lấy danh sách chatId của senderId
//        val senderQuery = chatMemberRef.orderByChild("userId").equalTo(senderId)
//        senderQuery.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(senderSnapshot: DataSnapshot) {
//                for (chat in senderSnapshot.children) {
//                    val chatMember = chat.getValue(ChatMember::class.java)
//                    if (chatMember != null) {
//                        senderChatIds.add(chatMember.chatId)
//                    }
//                }
//
//                if (senderChatIds.isEmpty()) {
//                    Log.e("FirebaseHelper", "Người gửi chưa tham gia chat nào")
//                    callback(null)
//                    return
//                }
//
//                // Lấy danh sách chatId của receiverId
//                val receiverQuery = chatMemberRef.orderByChild("userId").equalTo(receiverId)
//                receiverQuery.addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onDataChange(receiverSnapshot: DataSnapshot) {
//                        var chatId: String? = null
//
//                        for (chat in receiverSnapshot.children) {
//                            val chatMember = chat.getValue(ChatMember::class.java)
//                            if (chatMember != null && senderChatIds.contains(chatMember.chatId)) {
//                                chatId = chatMember.chatId
//                                break
//                            }
//                        }
//
//                        if (chatId != null) {
//                            // Kiểm tra số lượng thành viên trong chatId
//                            chatMemberRef.orderByChild("chatId").equalTo(chatId)
//                                .addListenerForSingleValueEvent(object : ValueEventListener {
//                                    override fun onDataChange(chatMemberSnapshot: DataSnapshot) {
//                                        if (chatMemberSnapshot.childrenCount == 2L) {
//                                            callback(chatId)
//                                        } else {
//                                            Log.e("FirebaseHelper", "ChatId $chatId có nhiều hơn 2 user, bỏ qua.")
//                                            callback(null)
//                                        }
//                                    }
//
//                                    override fun onCancelled(error: DatabaseError) {
//                                        Log.e("FirebaseHelper", "Lỗi khi kiểm tra số lượng thành viên: ${error.message}")
//                                        callback(null)
//                                    }
//                                })
//                        } else {
//                            Log.e("FirebaseHelper", "Không tìm thấy chatId giữa $senderId và $receiverId")
//                            callback(null)
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                        Log.e("FirebaseHelper", "Lỗi khi truy vấn receiverId: ${error.message}")
//                        callback(null)
//                    }
//                })
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("FirebaseHelper", "Lỗi khi truy vấn senderId: ${error.message}")
//                callback(null)
//            }
//        })
//    }

    // Lắng nghe toàn bộ tin nhắn cũ + tin nhắn mới
//    fun listenToAllMessages(
//        chatId: String,
//        onMessageReceived: (ChatMessage) -> Unit,
//        onUpdateMessage: (ChatMessage) -> Unit,
//        onDeleteMessage: (ChatMessage) -> Unit
//    ) {
//        chatMessageRef.orderByChild("chatId").equalTo(chatId)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    for (messageSnapshot in snapshot.children) {
//                        val chatMessage = messageSnapshot.getValue(ChatMessage::class.java)
//                        if (chatMessage != null) {
//                            fetchUserProfile(chatMessage, onMessageReceived)
//                        }
//                    }
//                    // Sau khi tải tin nhắn cũ, bắt đầu lắng nghe tin nhắn mới
//                    listenToChatMessages(chatId, onMessageReceived, onUpdateMessage, onDeleteMessage)
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("FirebaseHelper", "Lỗi khi tải tin nhắn cũ: ${error.message}")
//                }
//            })
//    }

    // Lắng nghe tin nhắn mới, cập nhật, xóa
//    private fun listenToChatMessages(
//        chatId: String,
//        onMessageReceived: (ChatMessage) -> Unit,
//        onUpdateMessage: (ChatMessage) -> Unit,
//        onDeleteMessage: (ChatMessage) -> Unit
//    ) {
//        chatMessageRef.orderByChild("chatId").equalTo(chatId)
//            .addChildEventListener(object : ChildEventListener {
//                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                    val chatMessage = snapshot.getValue(ChatMessage::class.java)
//                    if (chatMessage != null) {
//                        fetchUserProfile(chatMessage, onMessageReceived)
//                    }
//                }
//
//                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                    val updatedMessage = snapshot.getValue(ChatMessage::class.java)
//                    if (updatedMessage != null) {
//                        fetchUserProfile(updatedMessage, onUpdateMessage)
//                    }
//                }
//
//                override fun onChildRemoved(snapshot: DataSnapshot) {
//                    val removedMessage = snapshot.getValue(ChatMessage::class.java)
//                    if (removedMessage != null) {
//                        onDeleteMessage(removedMessage)
//                    }
//                }
//
//                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("FirebaseHelper", "Lỗi khi lắng nghe tin nhắn mới: ${error.message}")
//                }
//            })
//    }
//
//
//    // Lấy thông tin người gửi (avatar)
//    private fun fetchUserProfile(chatMessage: ChatMessage, callback: (ChatMessage) -> Unit) {
//        usersRef.child(chatMessage.senderId).get()
//            .addOnSuccessListener { userSnapshot ->
//                val user = userSnapshot.getValue(User::class.java)
//                chatMessage.senderAvatarUrl = user?.profileImage ?: ""
//                callback(chatMessage)
//            }
//            .addOnFailureListener {
//                Log.e("FirebaseHelper", "Không thể lấy thông tin người dùng: ${it.message}")
//                callback(chatMessage)
//            }
//    }


    fun listenForMessagesGroup(chatId:String ,
                                 onMessageReceived: (ChatMessage) -> Unit,
                                 onUpdateMessage: (ChatMessage) -> Unit,
                                 onDeleteMessage: (ChatMessage) -> Unit){
        chatMessageRef.orderByChild("chatId").equalTo(chatId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val chatMessage = snapshot.getValue(ChatMessage::class.java)
                    if (chatMessage != null) {
                        usersRef.child(chatMessage.senderId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val user = userSnapshot.getValue(User::class.java)
                                val senderImage = user?.profileImage ?: ""
                                chatMessage.senderAvatarUrl = senderImage
                                onMessageReceived(chatMessage)
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseHelper", "Không thể lấy thông tin người dùng: ${it.message}")
                                onMessageReceived(chatMessage)
                            }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val updatedMessage = snapshot.getValue(ChatMessage::class.java)
                    if (updatedMessage != null) {
                        usersRef.child(updatedMessage.senderId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val user = userSnapshot.getValue(User::class.java)
                                val senderImage = user?.profileImage ?: ""
                                updatedMessage.senderAvatarUrl = senderImage
                                onUpdateMessage(updatedMessage)
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseHelper", "Không thể lấy thông tin người dùng: ${it.message}")
                                onUpdateMessage(updatedMessage)
                            }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val removedMessage = snapshot.getValue(ChatMessage::class.java)
                    if (removedMessage != null) {
                        onDeleteMessage(removedMessage) // Gửi tin nhắn đã bị xóa về ViewModel
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseHelper", "Lỗi khi lắng nghe tin nhắn mới: ${error.message}")
                }
            })
    }

    fun getOldMessages(senderId: String?, receiverId: String, callback: (List<ChatMessage>) -> Unit) {
        // Tìm chatId chứa senderId
        chatMemberRef.orderByChild("userId").equalTo(senderId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var chatId: String? = null

                    for (chat in snapshot.children) {
                        val chatMember = chat.getValue(ChatMember::class.java)
                        if (chatMember != null) {
                            // Kiểm tra xem receiverId có trong cùng chatId không
                            chatMemberRef.orderByChild("chatId").equalTo(chatMember.chatId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(receiverSnapshot: DataSnapshot) {
                                        var memberCount = 0
                                        var isValidChat = false

                                        for (receiverChat in receiverSnapshot.children) {
                                            val receiverMember = receiverChat.getValue(ChatMember::class.java)
                                            if (receiverMember != null) {
                                                memberCount++

                                                // Nếu tìm thấy receiverId trong chatId này thì đánh dấu hợp lệ
                                                if (receiverMember.userId == receiverId) {
                                                    isValidChat = true
                                                }
                                            }
                                        }

                                        // Nếu chatId có đúng 2 người thì lấy tin nhắn
                                        if (isValidChat && memberCount == 2) {
                                            chatId = chatMember.chatId
                                            fetchOldMessages(chatId!!, callback)
                                        } else {
                                            callback(emptyList()) // Không phải chat cá nhân
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        callback(emptyList())
                                    }
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }


    fun saveImageUser(userId:String, imagePath: String, callback: (Boolean, String?) -> Unit){
        usersRef.child(userId).child("profileImage").setValue(imagePath)
            .addOnSuccessListener { callback(true, imagePath) }
            .addOnFailureListener { callback(false, null) }
    }

    fun getImageUser(userId:String?, callback: (String?) -> Unit){
        if(userId == null) return;
        usersRef.child(userId).child("profileImage").get().addOnSuccessListener { snapshot ->
            val imageUrl = snapshot.getValue(String::class.java) ?: ""
            callback(imageUrl)
        }
    }

    fun fetchOldMessages(chatId: String, callback: (List<ChatMessage>) -> Unit) {
        chatMessageRef.orderByChild("chatId").equalTo(chatId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<ChatMessage>()
                    for (child in snapshot.children) {
                        val message = child.getValue(ChatMessage::class.java)
                        message?.let {
                            usersRef.child(it.senderId).get()
                                .addOnSuccessListener { userSnapshot ->
                                    val user = userSnapshot.getValue(User::class.java)
                                    val senderImage = user?.profileImage ?: ""

                                    // Gán ảnh vào thuộc tính avatarUrl
                                    it.senderAvatarUrl = senderImage

                                }

                            messages.add(it)

                        }
                    }
                    // Loại bỏ tin nhắn mới nhất
                    if (messages.isNotEmpty()) {
                        messages.sortBy { it.timestamp }
                        messages.removeAt(messages.size - 1)
                    }
                    callback(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
    fun getChatInfo(chatId: String, callback: (Chat?) -> Unit) {
        chatsRef.child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatInfo = snapshot.getValue(Chat::class.java)
                callback(chatInfo)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun updateUserName(userId: String?, newName: String, callback: (Boolean) -> Unit) {
        if(userId == null) return

        val userRef = usersRef.child(userId)

        userRef.child("name").setValue(newName)
            .addOnSuccessListener {
                callback(true) // Thành công
            }
            .addOnFailureListener {
                callback(false) // Thất bại
            }
    }

    fun updateChatName(chatId: String, newChatName: String, callback:(Boolean) -> Unit) {
        val chatRef = chatsRef.child(chatId)

        chatRef.child("chatName").setValue(newChatName)
            .addOnSuccessListener {
                chatMemberRef.orderByChild("chatId").equalTo(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val chatMember = child.getValue(ChatMember::class.java)

                            if(chatMember != null){
                                usersRef.child(chatMember.userId).child("name").get()
                                    .addOnSuccessListener { receiverSnapshot ->
                                        val senderRecentChat = mapOf(
                                            "chatId" to chatId,
                                            "otherUserId" to chatMember.userId,
                                            "otherUserName" to newChatName,
                                            "lastMessage" to "Tên nhóm đã được thay đổi",
                                            "formattedTimestamp" to System.currentTimeMillis(),
                                            "group" to true,
                                            "isStatus" to true
                                        )

                                        // Cập nhật recent chat cho sender
                                        recentChat.child(chatMember.userId).child(chatId).get().addOnSuccessListener { snapshot ->
                                            if (!snapshot.exists()) {
                                                recentChat.child(chatMember.userId).child(chatId).setValue(senderRecentChat)
                                            } else {
                                                recentChat.child(chatMember.userId).child(chatId).updateChildren(senderRecentChat)
                                            }
                                        }
                                    }
                            }

                        }
                        callback(true)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseError", "Lỗi truy vấn: ${error.message}")
                        callback(false)
                    }
                })
                callback(true)

            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun leaveGroup(chatId: String, userId: String?, callback: (Boolean) -> Unit) {
        chatMemberRef.get().addOnSuccessListener { snapshot ->
            var isMemberRemoved = false

            for (child in snapshot.children) {
                val chatMember = child.getValue(ChatMember::class.java)

                if (chatMember != null && chatMember.chatId == chatId && chatMember.userId == userId) {
                    child.ref.removeValue()
                        .addOnSuccessListener {
                            isMemberRemoved = true
                            updateRecentChatOnLeave(chatId, userId)
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                    break
                }
            }

            if (!isMemberRemoved) {
                callback(false)
            }
        }.addOnFailureListener {
            Log.e("FirebaseError", "Lỗi truy vấn: ${it.message}")
            callback(false)
        }
    }


    private fun updateRecentChatOnLeave(chatId: String, userId: String) {
        // Xóa recent chat của user đã rời nhóm
        recentChat.child(userId).child(chatId).removeValue()
        usersRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    // Cập nhật tin nhắn hệ thống cho các thành viên còn lại
                    chatMemberRef.get().addOnSuccessListener { snapshot ->
                        for (child in snapshot.children) {
                            val chatMember = child.getValue(ChatMember::class.java)

                            if (chatMember != null && chatMember.chatId == chatId && chatMember.userId != userId) {
                                val recentUpdate = mapOf(
                                    "chatId" to chatId,
                                    "otherUserId" to chatMember.userId,
                                    "otherUserName" to "Nhóm",
                                    "lastMessage" to "${user.name} đã rời khỏi nhóm",
                                    "formattedTimestamp" to System.currentTimeMillis(),
                                    "group" to true,
                                    "isStatus" to true
                                )
                                recentChat.child(chatMember.userId).child(chatId).updateChildren(recentUpdate)
                            }
                        }
                    }.addOnFailureListener {
                        Log.e("FirebaseError", "Lỗi cập nhật Recent Chat: ${it.message}")
                    }
                }
            }
    }

    fun deleteGroup(chatId: String, callback: (Boolean) -> Unit) {
        chatMemberRef.get().addOnSuccessListener { snapshot ->
            val memberIds = mutableListOf<String>()

            for (child in snapshot.children) {
                val chatMember = child.getValue(ChatMember::class.java)
                if (chatMember != null && chatMember.chatId == chatId) {
                    memberIds.add(chatMember.userId)
                    child.ref.removeValue() // Xóa thành viên khỏi nhóm
                }
            }

            if (memberIds.isEmpty()) {
                callback(false) // Không có thành viên nào, không thể xóa nhóm
                return@addOnSuccessListener
            }

            // Xóa toàn bộ tin nhắn trong nhóm từ bảng `chats/{chatId}`
            chatsRef.child(chatId).removeValue()
                .addOnSuccessListener {
                    // Xóa recent chat của tất cả thành viên
                    for (userId in memberIds) {
                        recentChat.child(userId).child(chatId).removeValue()
                    }

                    // Xóa luôn thông tin nhóm khỏi bảng `chatMemberRef`
                    chatMemberRef.orderByChild("chatId").equalTo(chatId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (child in snapshot.children) {
                                    child.ref.removeValue()
                                }
                                callback(true) // Xóa thành công
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("FirebaseError", "Lỗi khi xóa thành viên nhóm: ${error.message}")
                                callback(false)
                            }
                        })
                }
                .addOnFailureListener {
                    callback(false)
                }
        }.addOnFailureListener {
            Log.e("FirebaseError", "Lỗi truy vấn: ${it.message}")
            callback(false)
        }
    }

}