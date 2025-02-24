package com.example.chatapp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.models.FriendRequest
import com.example.chatapp.models.User

class FriendRequestAdapter(
    private var friendRequests: List<Pair<FriendRequest, User?>>,
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onRejectClick: (FriendRequest) -> Unit
)  : RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder>(){

    class FriendRequestViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val userImage: ImageView = view.findViewById(R.id.userImage)
        val userName: TextView = view.findViewById(R.id.tvFriendTitle)
        val btnAccept: ImageButton = view.findViewById(R.id.btnAccept)
        val btnReject: ImageButton = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val friendRequest = friendRequests[position]

        holder.userName.text = friendRequest.second?.name
        Glide.with(holder.itemView.context)
            .load(friendRequest.second?.profileImage)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.baseline_account_circle_24)
            .into(holder.userImage)

        holder.btnAccept.setOnClickListener { onAcceptClick(friendRequest.first) }
        holder.btnReject.setOnClickListener { onRejectClick(friendRequest.first) }
    }

    override fun getItemCount() = friendRequests.size

    fun updateFriendRequest(newFriendRequest: List<Pair<FriendRequest, User?>>){
        friendRequests = newFriendRequest
        notifyDataSetChanged()
    }




}