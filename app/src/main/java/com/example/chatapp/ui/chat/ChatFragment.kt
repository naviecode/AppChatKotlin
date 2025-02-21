package com.example.chatapp.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.adapters.ActiveUsersAdapter
import com.example.chatapp.adapters.RecentChatsAdapter
import com.example.chatapp.adapters.UserSearchAdapter
import com.example.chatapp.databinding.FragmentChatBinding
import com.example.chatapp.models.User
import com.example.chatapp.models.UserWithFriendStatus
import com.example.chatapp.ui.profile.ProfileFragment
import com.example.chatapp.utils.FirebaseHelper
import com.example.chatapp.viewmodels.UserSearchViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!  // Tránh null khi truy cập
    private lateinit var activeUsersAdapter: ActiveUsersAdapter
    private lateinit var recentChatsAdapter: RecentChatsAdapter
    private lateinit var friendsAdapter: UserSearchAdapter
    private lateinit var strangersAdapter: UserSearchAdapter
    private val firebaseHelper = FirebaseHelper()
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private val viewModel: UserSearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseHelper.getImageUser(userId){imageUrl->
            Glide.with(this).load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.user_default_avatar)
                .error(R.drawable.user_default_avatar)
                .into(binding.userAvatarActiveCurrent)
        }
        setupRecyclerViews()
        loadActiveUsers()
        loadRecentChats()
        loadUserSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Tránh memory leak
    }


    private fun setupRecyclerViews(){
        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.strangersRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        friendsAdapter = UserSearchAdapter(emptyList()){ result ->
            startProfile(result)
        }
        strangersAdapter = UserSearchAdapter(emptyList()){ result ->
            startProfile(result)
        }

        binding.friendsRecyclerView.adapter = friendsAdapter

        binding.strangersRecyclerView.adapter = strangersAdapter

        activeUsersAdapter = ActiveUsersAdapter(emptyList()) {user ->
            startChat(user)
        }

        recentChatsAdapter = RecentChatsAdapter(emptyList()){recentChat ->
            startChat(User(recentChat.otherUserId, recentChat.otherUserName, profileImage =  recentChat.otherUserImage))
        }

        binding.activeUsersRecyclerView.adapter = activeUsersAdapter
        binding.recentChatsRecyclerView.apply {
            adapter = recentChatsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadActiveUsers(){
        firebaseHelper.getActiveFriends { users ->
            activeUsersAdapter.updateUsers(users)
        }
    }

    private fun loadRecentChats(){
        firebaseHelper.getRecentChats{ chats ->
            recentChatsAdapter.updateChats(chats)
        }
    }

    private fun loadUserSearch(){
        binding.searchUserEditText.addTextChangedListener { text ->

            val query = text.toString()
            viewModel.searchQuery.value = query

            if (query.isNotBlank()) {
                binding.listActiveUser.visibility = View.GONE
                binding.recentChatsRecyclerView.visibility = View.GONE
                binding.searchResultsLayout.visibility = View.VISIBLE
            } else {
                binding.listActiveUser.visibility = View.VISIBLE
                binding.recentChatsRecyclerView.visibility = View.VISIBLE
                binding.searchResultsLayout.visibility = View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.friends.collect { friends ->
                friendsAdapter.updateUsers(friends)
            }
        }
        lifecycleScope.launch {
            viewModel.strangers.collect { strangers ->
                strangersAdapter.updateUsers(strangers)
            }
        }
    }

    private fun startChat(user: User){
        val intent = Intent(requireContext(), ChatMessageActivity::class.java).apply {
            putExtra("userId", user.id)
        }
        startActivity(intent)
    }
    private fun startProfile(user: UserWithFriendStatus){
        val profileFragment = ProfileFragment().apply{
            arguments = Bundle().apply {
                putString("userId", user.userId)
            }
        }


        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, profileFragment)
            .addToBackStack(null)
            .commit()
    }
}