package com.example.chatapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.models.UserWithFriendStatus

class UserSearchAdapter(
    private var users: List<UserWithFriendStatus>,
    private val onUserClicked : (UserWithFriendStatus) -> Unit
)  : RecyclerView.Adapter<UserSearchAdapter.UserSearchHolder>() {

    class UserSearchHolder(view: View) : RecyclerView.ViewHolder(view){
        val userName: TextView = view.findViewById(R.id.usernameTextView)
        val email: TextView = view.findViewById(R.id.emailTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSearchHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserSearchHolder(view)
    }

    override fun onBindViewHolder(holder: UserSearchHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.userName
        holder.email.text = user.email

        holder.itemView.setOnClickListener{onUserClicked(user)}
    }

    override fun getItemCount(): Int {
        return  users.size
    }

    fun updateUsers(newUsers: List<UserWithFriendStatus>) {
        users = newUsers
        notifyDataSetChanged()
    }

}