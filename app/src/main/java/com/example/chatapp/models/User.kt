package com.example.chatapp.models

import android.net.Uri

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",
    val status: Boolean = false,
    val lastSeen: Long = 0
) {
    constructor() : this("", "", "", "", false, 0) // Constructor không tham số
}
