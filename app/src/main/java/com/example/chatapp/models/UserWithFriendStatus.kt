package com.example.chatapp.models

data class UserWithFriendStatus(
    val userId: String,
    val userName: String,
    val email: String,
    val isFriend: Boolean,
    val profileImage: String = ""
)