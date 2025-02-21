package com.example.chatapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.models.RecentChat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentChatsAdapter(
    private var recentChat: List<RecentChat>,
    private val onChatClicked: (RecentChat) -> Unit
) : RecyclerView.Adapter<RecentChatsAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.userImage)
        val userName: TextView = view.findViewById(R.id.userName)
        val lastMessage: TextView = view.findViewById(R.id.lastMessage)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val status: View = view.findViewById(R.id.statusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = recentChat[position]
        holder.userName.text = chat.otherUserName
        holder.lastMessage.text = chat.lastMessage
        holder.timestamp.text = FormatTime(chat.formattedTimestamp)

        if(chat.isStatus){
            holder.status.setBackgroundResource(R.drawable.online_status)
        }else{
            holder.status.setBackgroundResource(R.drawable.offline_status)
        }

        Glide.with(holder.itemView.context)
            .load(chat.otherUserImage)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.user_default_avatar)
            .into(holder.userImage)

        holder.itemView.setOnClickListener { onChatClicked(chat) }
    }

    fun FormatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun getItemCount() = recentChat.size

    fun updateChats(newChats: List<RecentChat>) {
        recentChat = newChats
        notifyDataSetChanged()
    }
}