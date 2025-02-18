package com.example.chatapp.models

data class Notification(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false
)
