package com.example.chatapp.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ProcessLifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivityMainChatBinding
import com.example.chatapp.firebase.AuthManager
import com.example.chatapp.models.UserWithFriendStatus
import com.example.chatapp.ui.friend_request.FriendRequestFragment
import com.example.chatapp.ui.profile.ProfileFragment
import com.example.chatapp.ui.user.CreateGroupDialog
import com.example.chatapp.utils.AppLifecycleObserver
import com.example.chatapp.utils.FirebaseHelper
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class MainChatActivity : AppCompatActivity(), CreateGroupDialog.OnGroupCreatedListener {
    private lateinit var binding: ActivityMainChatBinding
    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var authManager: AuthManager
    private val firebaseHelper = FirebaseHelper()
    private var handler: Handler? = null
    private var statusRunnable: Runnable? = null
    private lateinit var userList: List<UserWithFriendStatus>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        authManager = AuthManager(this)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        resetInactivityTimer()

        firebaseHelper.listenForNotifications { notification ->
            if(notification.senderId != currentUser && notification.senderId != ""){
                Toast.makeText(this,notification.message, Toast.LENGTH_SHORT).show()
            }
        }

        firebaseHelper.getUsersWithFriendStatus { users ->
            userList = users
        }

        binding.createGroup.setOnClickListener {
            CreateGroupDialog(userList, this, "CREATE").show(supportFragmentManager, "CreateGroupDialog")
        }

        replaceFragment(ChatFragment())
        setupProfileImage()
        setupEventClickNavBot()
    }
    override fun onGroupCreated(selectedUsers: List<UserWithFriendStatus>) {
        firebaseHelper.createGroupChat(selectedUsers){ result ->
            if(result){
                Toast.makeText(this, "Nhóm đã tạo với ${selectedUsers.size} thành viên", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Nhóm đã tạo thất bại", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameContainer, fragment)
        fragmentTransaction.commit()
    }

    private fun setupEventClickNavBot(){

        binding.icon1.setOnClickListener {
            binding.chatTitle.text = "Đoạn Chat"
            replaceFragment(ChatFragment())
        }

        binding.icon2.setOnClickListener {
            binding.chatTitle.text = "Lời mời kết bạn"
            replaceFragment(FriendRequestFragment())
        }

        binding.icon3.setOnClickListener {
            binding.chatTitle.text = "Thông tin người dùng"
            replaceFragment(ProfileFragment())
        }
    }

    private fun setupProfileImage() {

        firebaseHelper.getImageUser(currentUser) {imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.user_default_avatar)
                .error(R.drawable.user_default_avatar)
                .into(binding.profileImage)
        }


        binding.profileImage.setOnClickListener {
            showProfileOptions()
        }
    }

    private fun showProfileOptions() {
        // Hiện popup menu với các tùy chọn
        val popupMenu = PopupMenu(this, binding.profileImage)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.change_password -> {
                    showChangePasswordPopup()
                    true
                }
                R.id.logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun logout() {
        authManager.logout()
    }

    private fun resetInactivityTimer() {
        handler?.removeCallbacks(statusRunnable!!)

        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }

        statusRunnable = Runnable {
            firebaseHelper.updateUserStatus(false) // Sau 5 phút, đặt Offline
        }

        handler?.postDelayed(statusRunnable!!, 60000) // 5 phút
    }
    private fun showChangePasswordPopup() {
        // Sử dụng context thích hợp: nếu ở Activity dùng "this", nếu ở Fragment dùng "requireContext()"
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.change_password_popup, null)
        val oldPasswordEditText = dialogView.findViewById<EditText>(R.id.oldPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPasswordEditText)

        builder.setView(dialogView)
            .setTitle("Đổi mật khẩu")
            .setPositiveButton("Lưu") { dialog, _ ->
                val oldPassword = oldPasswordEditText.text.toString().trim()
                val newPassword = newPasswordEditText.text.toString().trim()

                if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                } else {
                    // Xác thực mật khẩu cũ và cập nhật mật khẩu mới
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && user.email != null) {
                        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                        user.reauthenticate(credential).addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this, "Đổi mật khẩu thất bại: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Mật khẩu cũ không chính xác", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        firebaseHelper.updateUserStatus(true) // Khi có thao tác, đặt Online
        resetInactivityTimer() // Reset lại bộ đếm
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseHelper.updateUserStatus(false) // Khi thoát ứng dụng, đặt Offline
    }
}