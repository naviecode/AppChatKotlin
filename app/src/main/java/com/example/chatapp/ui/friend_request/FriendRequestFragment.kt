package com.example.chatapp.ui.friend_request

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.chatapp.adapters.FriendRequestAdapter
import com.example.chatapp.databinding.FragmentFriendRequestBinding
import com.example.chatapp.models.FriendRequest
import com.example.chatapp.utils.FirebaseHelper

class FriendRequestFragment : Fragment() {
    private var _binding: FragmentFriendRequestBinding? = null
    private val binding get() = _binding!!
    private val firebaseHelper = FirebaseHelper()
    private lateinit var friendRequestAdapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        loadFriendRequest()

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Tránh memory leak
    }
    private fun setupRecyclerViews(){
        friendRequestAdapter = FriendRequestAdapter(emptyList(),
            onAcceptClick = { friendRequest ->
                handleAcceptFriendRequest(friendRequest)
            },
            onRejectClick = { friendRequest ->
                handleRejectFriendRequest(friendRequest)
            }
        )

        binding.friendRequestRecyclerView.adapter = friendRequestAdapter
    }

    private fun loadFriendRequest(){
        firebaseHelper.listenForFriendRequests { friendRequests ->
            friendRequestAdapter.updateFriendRequest(friendRequests)
        }
    }

    private fun handleAcceptFriendRequest(friendRequest: FriendRequest) {
        firebaseHelper.acceptFriendRequest(friendRequest.senderId) { success, message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                loadFriendRequest() // Cập nhật danh sách sau khi chấp nhận
            }
        }
    }

    private fun handleRejectFriendRequest(friendRequest: FriendRequest) {
        firebaseHelper.declineFriendRequest(friendRequest.senderId) { success, message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                loadFriendRequest() // Cập nhật danh sách sau khi từ chối
            }
        }
    }
    private fun startFriendRequest(friendRequest: FriendRequest){
        val intent = Intent(requireContext(), FriendRequestFragment::class.java).apply {

        }

        startActivity(intent)
    }
}