package com.example.chatapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.models.UserWithFriendStatus

class UserGroupChatAdapter (
    private var userList: List<UserWithFriendStatus>,
    private val onUserSelected: (UserWithFriendStatus, Boolean) -> Unit
) : RecyclerView.Adapter<UserGroupChatAdapter.UserGroupChatViewHolder>(){

    private val selectedUsers = mutableSetOf<String>()

    class UserGroupChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userName)
        val userAvatar: ImageView = view.findViewById(R.id.userAvatar)
        val addButton: ImageView = view.findViewById(R.id.addUserButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserGroupChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_group_chat, parent, false)
        return UserGroupChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserGroupChatViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.userName

        Glide.with(holder.itemView.context)
            .load(user.profileImage)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.user_default_avatar)
            .error(R.drawable.user_default_avatar)
            .into(holder.userAvatar)


        holder.addButton.setImageResource(
            if (selectedUsers.contains(user.userId)) R.drawable.tick_v3 else R.drawable.add
        )

        holder.addButton.setOnClickListener {
            val isSelected = selectedUsers.contains(user.userId)
            if (isSelected) {
                selectedUsers.remove(user.userId)
            } else {
                selectedUsers.add(user.userId)
            }
            onUserSelected(user, !isSelected)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<UserWithFriendStatus>) {
        userList = newList
        notifyDataSetChanged()
    }
}