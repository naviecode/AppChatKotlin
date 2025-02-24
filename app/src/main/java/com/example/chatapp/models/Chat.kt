package com.example.chatapp.models

import java.util.Date

data class Chat(
    val chatId: String = "",
    val chatName: String = "",
    val created: Date = Date(),
    val userCreatedId: String = "",
    val userCreatedName: String = "",
    val chatImg: String = "",
    val group: Boolean = false
)
