package com.example.chatapp.models

import java.util.Date

data class Chat(
    val chatId: String = "",
    val created: Date,
    val chatImg: String = "",
)
