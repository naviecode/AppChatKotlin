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
import com.example.chatapp.models.User

class ActiveUsersAdapter(
    private var users: List<User>,
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<ActiveUsersAdapter.UserViewHolder>() {


    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val userImage: ImageView = view.findViewById(R.id.userAvatar)
        val userName: TextView = view.findViewById(R.id.userName)
        val status: View = view.findViewById(R.id.statusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.name
        if(user.status) {
            holder.status.setBackgroundResource(R.drawable.online_status)
        }else{
            holder.status.setBackgroundResource(R.drawable.offline_status)
        }
        Glide.with(holder.itemView.context)
            .load(user.profileImage)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.baseline_account_circle_24)
            .into(holder.userImage)

        holder.itemView.setOnClickListener{onUserClicked(user)}
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>){
        users = newUsers
        notifyDataSetChanged()
    }
}