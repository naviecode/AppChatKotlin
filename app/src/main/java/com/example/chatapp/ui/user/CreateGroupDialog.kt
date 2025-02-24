package com.example.chatapp.ui.user

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.adapters.UserGroupChatAdapter
import com.example.chatapp.models.UserWithFriendStatus

class CreateGroupDialog(private val users: List<UserWithFriendStatus>,  private val listener: OnGroupCreatedListener, private val typeChat: String ="CREATE") : DialogFragment() {

    private lateinit var adapter: UserGroupChatAdapter
    private val selectedUsers = mutableListOf<UserWithFriendStatus>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.group_chat_popup, container, false)

        val searchEditText = view.findViewById<EditText>(R.id.searchUser)
        val recyclerView = view.findViewById<RecyclerView>(R.id.userListView)
        val btnCreate = view.findViewById<Button>(R.id.btnCreate)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val title = view.findViewById<TextView>(R.id.tvGroupTitle)

        if(typeChat == "CREATE"){
            btnCreate.text = "Tạo"
            title.text = "Tạo nhóm chat"
        }else{
            btnCreate.text = "Thêm"
            title.text = "Thêm thành viên"
        }
        adapter = UserGroupChatAdapter(users) { user, isSelected ->
            if (isSelected) selectedUsers.add(user) else selectedUsers.remove(user)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filteredList = users.filter { it.userName.contains(s.toString(), ignoreCase = true) }
                adapter.updateList(filteredList)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnCreate.setOnClickListener {
            if(typeChat == "CREATE"){
                if (selectedUsers.isNotEmpty() && selectedUsers.size > 1) {
                    listener.onGroupCreated(selectedUsers)
                    dismiss()
                } else {
                    Toast.makeText(context, "Vui lòng chọn ít nhất hai thành viên!", Toast.LENGTH_SHORT).show()
                }
            }else{
                if (selectedUsers.isNotEmpty() && selectedUsers.size > 0) {
                    listener.onGroupCreated(selectedUsers)
                    dismiss()
                } else {
                    Toast.makeText(context, "Vui lòng chọn ít nhất một thành viên!", Toast.LENGTH_SHORT).show()
                }
            }

        }

        btnCancel.setOnClickListener { dismiss() }

        return view
    }

    interface OnGroupCreatedListener {
        fun onGroupCreated(selectedUsers: List<UserWithFriendStatus>)
    }
}