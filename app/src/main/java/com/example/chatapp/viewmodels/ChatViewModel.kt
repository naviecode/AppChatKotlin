package com.example.chatapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.utils.FirebaseHelper
import com.google.firebase.messaging.remoteMessage

class ChatViewModel : ViewModel()  {
    private val firebaseHelper = FirebaseHelper()
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> get() = _messages

    private val _oldMessages = MutableLiveData<List<ChatMessage>>()
    val oldMessages: LiveData<List<ChatMessage>> = _oldMessages

    fun startListening(senderId: String?, receiverId: String) {
        firebaseHelper.listenForMessages(senderId, receiverId,
            onMessageReceived =  { message -> addMessage(message)},
            onUpdateMessage = {message -> updateMessage(message)},
            onDeleteMessage = {message -> removeMessage(message)}
        )
    }
    fun fetchOldMessages(senderId: String?, receiverId: String) {
        firebaseHelper.getOldMessages(senderId, receiverId) { messages ->
            _oldMessages.postValue(messages)
        }
    }

    private  fun addMessage(addMessage: ChatMessage){
        val currentList = _messages.value.orEmpty().toMutableList()
        currentList.add(addMessage)
        _messages.postValue(currentList)
    }


    private fun removeMessage(removedMessage: ChatMessage) {
        val currentList = _messages.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.chatMessageId == removedMessage.chatMessageId }
        if (index != -1) {
            currentList.removeAt(index)
            _messages.postValue(currentList)
        }
    }

    private fun updateMessage(updatedMessage: ChatMessage) {
        val currentList = _messages.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.chatMessageId == updatedMessage.chatMessageId }
        if (index != -1) {
            currentList[index] = updatedMessage
            _messages.postValue(currentList)
        }
    }

}