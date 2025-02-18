package com.example.chatapp.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ProcessLifecycleOwner
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivityMainChatBinding
import com.example.chatapp.firebase.AuthManager
import com.example.chatapp.ui.friend_request.FriendRequestFragment
import com.example.chatapp.ui.profile.ProfileFragment
import com.example.chatapp.utils.AppLifecycleObserver
import com.example.chatapp.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth

class MainChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainChatBinding
    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var authManager: AuthManager
    private val firebaseHelper = FirebaseHelper()
    private var handler: Handler? = null
    private var statusRunnable: Runnable? = null

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
            if(notification.senderId != currentUser){
                Toast.makeText(this,"Bạn có lời mời kết bạn", Toast.LENGTH_SHORT).show()

            }
        }

        replaceFragment(ChatFragment())
        setupProfileImage()
        setupEventClickNavBot()
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameContainer, fragment)
        fragmentTransaction.commit()
    }

    private fun setupEventClickNavBot(){

        binding.icon1.setOnClickListener {
            replaceFragment(ChatFragment())
        }

        binding.icon2.setOnClickListener {
            replaceFragment(FriendRequestFragment())
        }

        binding.icon3.setOnClickListener {
            replaceFragment(ProfileFragment())
        }
    }

    private fun setupProfileImage() {
        Glide.with(this)
            .load("")
            .placeholder(R.drawable.baseline_account_circle_24)
            .error(R.drawable.baseline_account_circle_24)
            .into(binding.profileImage)

        binding.profileImage.setOnClickListener {
            // Hiển thị menu với các tùy chọn đổi mật khẩu và đăng xuất
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
                    // Xử lý đổi mật khẩu
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