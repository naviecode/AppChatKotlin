package com.example.chatapp.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class AppLifecycleObserver : LifecycleObserver {
    private val firebaseHelper = FirebaseHelper()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        firebaseHelper.updateUserStatus(true) // Khi app mở
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        firebaseHelper.updateUserStatus(false) // Khi app đóng
    }
}