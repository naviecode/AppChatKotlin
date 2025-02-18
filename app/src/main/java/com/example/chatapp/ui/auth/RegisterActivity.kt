package com.example.chatapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.databinding.ActivityRegisterBinding
import com.example.chatapp.firebase.AuthManager

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                authManager.register(email, password, name, {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }, { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()

                })
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }
    }
}