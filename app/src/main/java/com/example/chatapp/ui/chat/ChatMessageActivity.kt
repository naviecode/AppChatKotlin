package com.example.chatapp.ui.chat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.adapters.ChatAdapter
import com.example.chatapp.databinding.ActivityChatMessageBinding
import com.example.chatapp.utils.FirebaseHelper
import com.example.chatapp.viewmodels.ChatViewModel
import com.example.chatapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class ChatMessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatMessageBinding
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val firebaseHelper = FirebaseHelper()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var receiverId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]


        //userId được nhận tin nhắn
        receiverId = intent.getStringExtra("userId") ?: ""

        userViewModel.fetchUser(receiverId)
        userViewModel.user.observe(this) { user ->
            binding.userName.text = user.name
        }


        setupRecyclerView()
        loadMessage()
        setupButton()
    }

    private fun setupRecyclerView(){
        chatAdapter = ChatAdapter(mutableListOf(),mutableListOf(), currentUserId)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun loadMessage(){

        //Load data first
        chatViewModel.fetchOldMessages(currentUserId, receiverId)
        chatViewModel.oldMessages.observe(this){oldMessages->
            chatAdapter.setOldMessages(oldMessages)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }

        //Load data message realtime
        chatViewModel.startListening(currentUserId, receiverId)
        chatViewModel.messages.observe(this) { messages ->
            chatAdapter.addNewMessage(messages)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }

    }

    private fun setupButton(){
        binding.sendMessageButton.setOnClickListener{
           val messageText = binding.messageInput.text.toString()
           if(messageText.isNotEmpty()){
               firebaseHelper.sendMessage(currentUserId, receiverId, messageText)
               binding.messageInput.setText("")

           }
        }

        binding.backButton.setOnClickListener(){
            val intent = Intent(this, MainChatActivity::class.java).apply {
                //truyền field
            }
            startActivity(intent)
        }

    }
}