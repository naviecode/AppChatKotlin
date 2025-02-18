package com.example.chatapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatapp.models.User
import com.example.chatapp.utils.FirebaseHelper

class UserViewModel : ViewModel() {
    private val firebaseHelper = FirebaseHelper()
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    fun fetchUser(userId: String?) {
        if (userId.isNullOrEmpty()) {
            Log.e("UserViewModel", "UserId is null or empty")
            return
        }

        firebaseHelper.getUserById(userId, { fetchedUser ->
            _user.value = fetchedUser
        }, { error ->
            Log.e("UserViewModel", "Error: $error")
        })
    }
}