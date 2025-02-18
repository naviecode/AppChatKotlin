package com.example.chatapp.firebase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.chatapp.models.User
import com.example.chatapp.ui.auth.LoginActivity
import com.example.chatapp.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    fun register(email: String, password: String, name: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(context as Activity) { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                    saveUserToRealtimeDB(userId, name, email, onSuccess, onFailure)
                    saveUserToFirestore(userId, name, email, onSuccess, onFailure)
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Đăng ký thất bại")
                }
            }
    }

    private fun saveUserToFirestore(userId: String, name: String, email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val user = User(
            id = userId,
            name = name,
            email = email,
            profileImage = "",  // Chưa có ảnh thì để rỗng
            status = false,
            lastSeen = System.currentTimeMillis()
        )

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi khi lưu user: ${e.message}")
            }
    }

    private fun saveUserToRealtimeDB(
        userId: String,
        name: String,
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users") // Node "users"

        val user = User(
            id = userId,
            name = name,
            email = email,
            profileImage = "",  // Chưa có ảnh thì để rỗng
            status = false,
            lastSeen = System.currentTimeMillis()
        )

        usersRef.child(userId).setValue(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi khi lưu user: ${e.message}")
            }
    }



    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(context as Activity) { task ->
                if (task.isSuccessful) {
                    firebaseHelper.updateUserStatus(true) // Cập nhật trạng thái online

                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Đăng nhập thất bại")
                }
            }
    }

    fun logout() {
        firebaseHelper.updateUserStatus(false) // Cập nhật trạng thái offline
        auth.signOut()
        Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginActivity::class.java))
        (context as Activity).finish()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}