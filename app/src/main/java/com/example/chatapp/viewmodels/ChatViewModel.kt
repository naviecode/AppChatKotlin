package com.example.chatapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.utils.FirebaseHelper

class ChatViewModel : ViewModel()  {
    private val firebaseHelper = FirebaseHelper()
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> get() = _messages

    private val _oldMessages = MutableLiveData<List<ChatMessage>>()
    val oldMessages: LiveData<List<ChatMessage>> = _oldMessages

    fun startListening(senderId: String?, receiverId: String) {
        firebaseHelper.listenForMessages(senderId, receiverId) { message ->
            val currentList = _messages.value.orEmpty().toMutableList()
            currentList.add(message)
            _messages.postValue(currentList)
        }
    }
    fun fetchOldMessages(senderId: String?, receiverId: String) {
        firebaseHelper.getOldMessages(senderId, receiverId) { messages ->
            _oldMessages.postValue(messages)
        }
    }

}