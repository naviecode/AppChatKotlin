package com.example.chatapp.models

data class ChatMessage(
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val senderAvatarUrl: String = ""
)