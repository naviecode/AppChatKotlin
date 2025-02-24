package com.example.chatapp.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.adapters.ChatAdapter
import com.example.chatapp.databinding.ActivityChatMessageBinding
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.models.UserWithFriendStatus
import com.example.chatapp.ui.user.CreateGroupDialog
import com.example.chatapp.utils.FirebaseHelper
import com.example.chatapp.viewmodels.ChatViewModel
import com.example.chatapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class ChatMessageActivity : AppCompatActivity(), CreateGroupDialog.OnGroupCreatedListener {
    private lateinit var binding: ActivityChatMessageBinding
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val firebaseHelper = FirebaseHelper()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var receiverId: String
    private lateinit var chatId: String
    private var isGroup: Boolean = false
    private lateinit var userList: List<UserWithFriendStatus>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]



        chatId = intent.getStringExtra("chatId") ?: ""
        receiverId = intent.getStringExtra("userId") ?: ""
        firebaseHelper.getChatInfo(chatId){result ->
            if(result != null){
                isGroup = result.group
                if(chatId != "" && result.group){
                    binding.userName.text = result.chatName
                    Glide.with(this)
                        .load(result.chatImg)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.group_chat_default)
                        .error(R.drawable.group_chat_default)
                        .into(binding.userAvatar)
                    firebaseHelper.getUsersWithFriendStatus { users ->
                        userList = users
                    }
                    loadMessageGroup()
                    setupButtonGroup()
                }
                else{
                    userViewModel.fetchUser(receiverId)
                    userViewModel.user.observe(this) { user ->
                        binding.userName.text = user.name
                        Glide.with(this)
                            .load(user.profileImage)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.user_default_avatar)
                            .error(R.drawable.user_default_avatar)
                            .into(binding.userAvatar)
                    }

                    loadMessage()
                    setupButton()
                }
            }
        }
        //Nếu chatId khác null và isGroup = true
        setupRecyclerView()



    }

    private fun setupRecyclerView(){
        chatAdapter = ChatAdapter(mutableListOf(),mutableListOf(), currentUserId){textView, chatmessage ->
            if(chatmessage.senderId == currentUserId){
                showMessageOptions(textView,this, chatmessage)
            }
        }
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun showMessageOptions(anchor:View, context: Context, message: ChatMessage) {
        val popupMenu = PopupMenu(context, anchor)
        popupMenu.menu.add("Chỉnh sửa")
        popupMenu.menu.add("Xóa")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Chỉnh sửa" -> editMessage(context, message)
                "Xóa" -> deleteMessage(context, message)
            }
            true
        }

        popupMenu.show()
    }

    private fun editMessage(context: Context, message: ChatMessage) {
        val editText = EditText(context)
        editText.setText(message.text)

        AlertDialog.Builder(context)
            .setTitle("Chỉnh sửa tin nhắn")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val newText = editText.text.toString()
                firebaseHelper.updateMessageText(message.chatMessageId, newText, currentUserId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Hàm xử lý xóa tin nhắn
    private fun deleteMessage(context: Context, message: ChatMessage) {
        AlertDialog.Builder(context)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa tin nhắn này?")
            .setPositiveButton("Xóa") { _, _ ->
                firebaseHelper.deleteMessage(message.chatMessageId, currentUserId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadMessage(){

        //Load data first
        chatViewModel.fetchOldMessages(currentUserId, receiverId)
        chatViewModel.oldMessages.observe(this){oldMessages->
            chatAdapter.setOldMessages(oldMessages)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }

        //Load data message realtime
        chatViewModel.startListening(currentUserId, receiverId)
        chatViewModel.messages.observe(this) { messages ->
            chatAdapter.addNewMessage(messages)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }

    }

    private fun setupButton(){
        binding.sendMessageButton.setOnClickListener{
           val messageText = binding.messageInput.text.toString()
           if(messageText.isNotEmpty()){
               firebaseHelper.sendMessage(currentUserId, receiverId, messageText)
               binding.messageInput.setText("")

           }
        }
        binding.backButton.setOnClickListener(){
            finish()
        }

    }

    private fun loadMessageGroup(){
        //Load data first
        chatViewModel.fetchOldMessagesGroup(chatId)
        chatViewModel.oldMessages.observe(this){oldMessages->
            chatAdapter.setOldMessages(oldMessages)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }

        //Load data message realtime
        chatViewModel.startListeningGroup(chatId)
        chatViewModel.messages.observe(this) { messages ->
            chatAdapter.addNewMessage(messages)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }
    private fun setupButtonGroup(){
        binding.sendMessageButton.setOnClickListener{
            val messageText = binding.messageInput.text.toString()
            if(messageText.isNotEmpty()){
                firebaseHelper.sendMessageGroup(currentUserId, chatId, messageText)
                binding.messageInput.setText("")

            }
        }
        binding.menuButton.visibility = View.VISIBLE
        binding.menuButton.setOnClickListener { view ->
            showChatMenu(view)
        }

        binding.backButton.setOnClickListener(){
            finish()
        }
    }

    override fun onGroupCreated(selectedUsers: List<UserWithFriendStatus>) {
        firebaseHelper.addGroupChat(selectedUsers, chatId){sucess, messge ->
            if(sucess){
                Toast.makeText(this, "Thêm thành viên thành công", Toast.LENGTH_SHORT).show()

            }else{
                Toast.makeText(this, "${messge} đã có tồn tại nhóm chat", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun showChatMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.chat_group_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.renameGroup -> {
                    showRenameGroupDialog(chatId)
                    true
                }
                R.id.addMember -> {
                    CreateGroupDialog(userList, this, "ADD").show(supportFragmentManager, "CreateGroupDialog")
                    true
                }
                R.id.deleteGroup -> {
                    showDeleteGroupDialog(chatId)
                    true
                }
                R.id.leaveGroup -> {
                    showLeaveGroupDialog(chatId, currentUserId)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun showRenameGroupDialog(groupId: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Đổi tên nhóm")

        val input = EditText(this)
        input.hint = "Nhập tên mới"
        dialogBuilder.setView(input)

        dialogBuilder.setPositiveButton("Xác nhận") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateGroupName(groupId, newName)
            } else {
                Toast.makeText(this, "Tên nhóm không được để trống", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.show()
    }

    private fun updateGroupName(groupId: String, newName: String) {
        firebaseHelper.updateChatName(groupId, newName){result ->
            if(result){
                binding.userName.text = newName
                Toast.makeText(this, "Đã đổi tên nhóm", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Lỗi khi đổi tên nhóm", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteGroupDialog(chatId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Xóa nhóm")
        builder.setMessage("Bạn có chắc muốn xóa nhóm này không? Hành động này không thể hoàn tác.")

        builder.setPositiveButton("Xóa") { _, _ ->
            deleteGroup(chatId)
        }
        builder.setNegativeButton("Hủy", null)

        builder.show()
    }

    private fun deleteGroup(chatId: String) {
        firebaseHelper.getChatInfo(chatId) {result ->
            if(result != null){
                if(result.userCreatedId != currentUserId){
                    Toast.makeText(this, "Chỉ có người tạo mới được xóa nhóm", Toast.LENGTH_SHORT).show()
                }else{
                    firebaseHelper.deleteGroup(chatId){result ->
                        if(result){
                            binding.sendMessageButton.visibility = View.GONE
                            binding.messageInput.visibility = View.GONE
                            binding.menuButton.visibility = View.GONE
                            Toast.makeText(this, "Bạn đã xóa nhóm", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this, "Xóa nhóm thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        }

    }
    private fun showLeaveGroupDialog(chatId: String, userId: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rời nhóm")
        builder.setMessage("Bạn có chắc muốn rời nhóm này không?")

        builder.setPositiveButton("Rời nhóm") { _, _ ->
            leaveGroup(chatId, userId)
        }
        builder.setNegativeButton("Hủy", null)

        builder.show()
    }

    private fun leaveGroup(chatId: String, userId: String?) {
        firebaseHelper.getChatInfo(chatId) {result ->
            if(result != null){
                if(result.userCreatedId == currentUserId){
                    Toast.makeText(this, "Bạn là trưởng nhóm nên không thể rời khỏi nhóm", Toast.LENGTH_SHORT).show()
                }
                else{
                    firebaseHelper.leaveGroup(chatId, userId) {result ->
                        if(result){
                            binding.sendMessageButton.visibility = View.GONE
                            binding.messageInput.visibility = View.GONE
                            binding.menuButton.visibility = View.GONE
                            Toast.makeText(this, "Bạn đã rời khỏi nhóm", Toast.LENGTH_SHORT).show()

                        }else{
                            Toast.makeText(this, "Rời khỏi nhóm thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    }


}