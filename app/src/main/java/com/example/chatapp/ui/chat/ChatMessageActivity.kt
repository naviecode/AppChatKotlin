package com.example.chatapp.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.adapters.ChatAdapter
import com.example.chatapp.databinding.ActivityChatMessageBinding
import com.example.chatapp.models.ChatMessage
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
            Glide.with(this)
                .load(user.profileImage)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.user_default_avatar)
                .error(R.drawable.user_default_avatar)
                .into(binding.userAvatar)
        }

        setupRecyclerView()
        loadMessage()
        setupButton()
    }

    private fun setupRecyclerView(){
        chatAdapter = ChatAdapter(mutableListOf(),mutableListOf(), currentUserId){textView, chatmessage ->
            if(chatmessage.senderId == currentUserId){
                showMessageOptions(textView,this, chatmessage)
            }
        }
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun showMessageOptions(anchor:View, context: Context, message: ChatMessage) {
        val popupMenu = PopupMenu(context, anchor)
        popupMenu.menu.add("Chỉnh sửa")
        popupMenu.menu.add("Xóa")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Chỉnh sửa" -> editMessage(context, message)
                "Xóa" -> deleteMessage(context, message)
            }
            true
        }

        popupMenu.show()
    }

    private fun editMessage(context: Context, message: ChatMessage) {
        val editText = EditText(context)
        editText.setText(message.text)

        AlertDialog.Builder(context)
            .setTitle("Chỉnh sửa tin nhắn")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val newText = editText.text.toString()
                firebaseHelper.updateMessageText(message.chatMessageId, newText, currentUserId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Hàm xử lý xóa tin nhắn
    private fun deleteMessage(context: Context, message: ChatMessage) {
        AlertDialog.Builder(context)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa tin nhắn này?")
            .setPositiveButton("Xóa") { _, _ ->
                firebaseHelper.deleteMessage(message.chatMessageId, currentUserId)
            }
            .setNegativeButton("Hủy", null)
            .show()
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
            finish()
        }

    }
}