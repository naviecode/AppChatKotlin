package com.example.chatapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.models.ChatMessage

class ChatAdapter(
    private var messageOld: MutableList<ChatMessage>,
    private var messageNew: MutableList<ChatMessage>,
    private val currentUserId: String?,
    private val onTouchMessage: (TextView,ChatMessage) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    private fun getAllMessages(): List<ChatMessage> {
        return messageOld + messageNew
    }


    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
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
        Glide.with(holder.itemView.context)
            .load(message.senderAvatarUrl.ifEmpty { R.drawable.user_default_avatar })
            .apply(RequestOptions.circleCropTransform())
            .into(holder.userAvatar)

        holder.itemView.setOnTouchListener(object : View.OnTouchListener {
            private var handler = Handler(Looper.getMainLooper())
            private val longPressRunnable = Runnable{
                onTouchMessage(holder.messageText, message)
            }
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handler.postDelayed(longPressRunnable, 500) // Nhấn giữ 500ms để kích hoạt sự kiện
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable) // Hủy nếu nhả tay sớm
                    }
                }
                return true
            }
            
        })
    }

    override fun getItemViewType(position: Int): Int {
        return if (getAllMessages()[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }


    override fun getItemCount() = getAllMessages().size

    fun setOldMessages(oldMessages: List<ChatMessage>) {
        messageOld.clear()
        messageOld.addAll(oldMessages)
        notifyDataSetChanged()
    }
    fun addNewMessage(newMessage: List<ChatMessage>) {
        messageNew.clear()
        messageNew.addAll(newMessage)
        notifyDataSetChanged()
    }
}

