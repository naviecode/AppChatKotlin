package com.example.chatapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.databinding.ActivityLoginBinding
import com.example.chatapp.firebase.AuthManager
import com.example.chatapp.ui.chat.MainChatActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)

        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                binding.tvError.text = "Vui lòng nhập đầy đủ thông tin!"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            authManager.login(email, password, {
                startActivity(Intent(this, MainChatActivity::class.java))
                finish()
            }, { error ->
                binding.tvError.text = "Tên đăng nhập hoặc mật khẩu bị sai"
                binding.tvError.visibility = View.VISIBLE
            })
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}