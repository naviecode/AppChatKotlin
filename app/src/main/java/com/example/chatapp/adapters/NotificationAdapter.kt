package com.example.chatapp.adapters

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter {
    private val database = FirebaseDatabase.getInstance().reference

    fun sendNotification(receiverId: String, senderId: String, type: String, content: String){
        val notificationRef = database.child("notifications").child(receiverId).push()

        val notificationData = mapOf(
            "type" to type,
            "senderId" to senderId,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )

        notificationRef.setValue(notificationData).addOnSuccessListener {
            Log.d("Notification", "Thông báo đã được gửi: $type")
        }.addOnFailureListener{e->
            Log.e("Notification", "Lỗi gửi thông báo: ${e.message}")
        }
    }
}