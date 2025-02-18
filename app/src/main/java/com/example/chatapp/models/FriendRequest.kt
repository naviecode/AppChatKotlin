package com.example.chatapp.models

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED
}
