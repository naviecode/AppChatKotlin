package com.example.chatapp.models

data class ChatMessage(
    val chatMessageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    var senderName:String = "",
    val text: String = "",
    val timestamp: Long = 0,
    var senderAvatarUrl: String = ""
)