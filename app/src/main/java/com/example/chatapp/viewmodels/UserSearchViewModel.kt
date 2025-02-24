package com.example.chatapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.models.User
import com.example.chatapp.models.UserWithFriendStatus
import com.example.chatapp.utils.FirebaseHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserSearchViewModel : ViewModel() {
    val searchQuery = MutableStateFlow("")
    private val _friends = MutableStateFlow<List<UserWithFriendStatus>>(emptyList())
    private val _strangers = MutableStateFlow<List<UserWithFriendStatus>>(emptyList())
    private val firebaseHelper = FirebaseHelper()
    val friends: StateFlow<List<UserWithFriendStatus>> = _friends
    val strangers: StateFlow<List<UserWithFriendStatus>> = _strangers

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank()) {
                        val result = searchUsers(query)
                        _friends.value = result.filter { it.isFriend }
                        _strangers.value = result.filter { !it.isFriend }
                    } else {
                        _friends.value = emptyList()
                        _strangers.value = emptyList()
                    }
                }
        }
    }

    private suspend fun searchUsers(query: String): List<UserWithFriendStatus> {
        delay(500)
        return suspendCoroutine { continuation ->
            firebaseHelper.getUsers { users ->
                val filteredUsers = users.filter { it.userName.contains(query, ignoreCase = true) }
                continuation.resume(filteredUsers)
            }
        }
    }

}