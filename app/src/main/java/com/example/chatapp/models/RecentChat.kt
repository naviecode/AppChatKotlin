package com.example.chatapp.models

data class RecentChat(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String = "",
    val lastMessage: String,
    val formattedTimestamp: Long,
    val otherUserImage: String = "",
    val isStatus: Boolean = false,
    val group: Boolean = false
){
    // Constructor không tham số
    constructor() : this("", "", "", "", 0)
}