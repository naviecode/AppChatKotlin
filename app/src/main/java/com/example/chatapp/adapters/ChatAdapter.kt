package com.example.chatapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.models.ChatMessage

class ChatAdapter(
    private var messageOld: MutableList<ChatMessage>,
    private var messageNew: MutableList<ChatMessage>,
    private val currentUserId: String?
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    private fun getAllMessages(): List<ChatMessage> {
        return messageOld + messageNew
    }
    // ViewHolder bên trong Adapter
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
//        val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
    }

    // Tạo ViewHolder dựa trên kiểu tin nhắn
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = if (viewType == VIEW_TYPE_SENT) {
            inflater.inflate(R.layout.item_message_sent, parent, false)
        } else {
            inflater.inflate(R.layout.item_message_received, parent, false)
        }
        return ChatViewHolder(view)
    }

    // Bind dữ liệu vào ViewHolder
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = getAllMessages()[position]
        holder.messageText.text = message.text
//        Glide.with(holder.itemView.context)
//            .load(message.senderAvatarUrl.ifEmpty { R.drawable.user_default_avatar })
//            .into(holder.userAvatar)
    }

    // Xác định kiểu tin nhắn: Sent hay Received
    override fun getItemViewType(position: Int): Int {
        return if (getAllMessages()[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }


    override fun getItemCount() = getAllMessages().size

    // Cập nhật tin nhắn cũ (load lần đầu)
    fun setOldMessages(oldMessages: List<ChatMessage>) {
        messageOld.clear()
        messageOld.addAll(oldMessages)
        notifyDataSetChanged()
    }

    // Cập nhật tin nhắn mới (khi có tin nhắn mới)
    fun addNewMessage(newMessage: List<ChatMessage>) {
        messageNew.clear()
        messageNew.addAll(newMessage)
        notifyDataSetChanged()
    }
}

