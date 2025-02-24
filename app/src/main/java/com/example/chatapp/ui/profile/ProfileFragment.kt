package com.example.chatapp.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentProfileBinding
import com.example.chatapp.utils.FirebaseHelper
import com.example.chatapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private val usersRef: DatabaseReference =  FirebaseDatabase.getInstance().getReference("users")
    private val firebaseHelper = FirebaseHelper()

    private lateinit var userViewModel: UserViewModel
    private var isFriend = false
    private var requestSent = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        arguments?.let {
            userId = it.getString("userId")
        }
        userViewModel.fetchUser(userId)

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (!user.name.isNullOrEmpty() && !user.email.isNullOrEmpty()) {
                binding.userNameTextView.text = user.name
                binding.emailTextView.text = user.email
                binding.userNameTextView.visibility = View.VISIBLE
                binding.emailTextView.visibility = View.VISIBLE
            }
        }

        firebaseHelper.countFriends{result ->
            binding.friendCountTextView.text = "$result bạn bè"
        }

        // Tải ảnh đại diện nếu có
        loadImage()

        // Chọn ảnh từ thư viện
        binding.profileImageView.setOnClickListener {
            openGallery()
        }

        binding.editNameButton.setOnClickListener{
            showEditNamePopup()
        }

        loadFriendRequest()

        updateUI()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Tránh memory leak
    }

    private fun loadFriendRequest(){

        if(userId != FirebaseAuth.getInstance().currentUser?.uid)
        {
            firebaseHelper.areFriends(userId, FirebaseAuth.getInstance().currentUser?.uid) {isFriendResult ->
                isFriend = isFriendResult
                updateUI()
            }
            firebaseHelper.hasPendingFriendRequest(FirebaseAuth.getInstance().currentUser?.uid, userId){isRequestResult ->
                requestSent = isRequestResult
                updateUI()
            }

            binding.friendActionButton.setOnClickListener {
                if (!isFriend && !requestSent) {
                    firebaseHelper.sendFriendRequest(userId) { success, message ->
                        if (success) {
                            requestSent = true
                            updateUI()
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } else if (isFriend) {
                    firebaseHelper.cancelFriendship(userId) { success, message ->
                        if (success) {
                            isFriend = false
                            requestSent = false
                            updateUI()
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri) { imageUrl ->
                    if (imageUrl != null) {
                        Log.d("Cloudinary", "Uploaded Image URL: $imageUrl")
                        loadImage()
                    } else {
                        Log.e("Cloudinary", "Upload failed")
                    }
                }
            }
        }
    }

    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String?) -> Unit) {
        val currentUserId = userId ?: return
        val cloudName = "db4jiiw1w"
        val apiKey = "898543479459721"
        val apiSecret = "zkMPsLuGg_GqTsnf8qyEsbDkEQ0"

        val cloudinary = Cloudinary("cloudinary://$apiKey:$apiSecret@$cloudName")

        val inputStream = requireContext().contentResolver.openInputStream(imageUri)

        // Chuyển ảnh thành byte array
        val byteArray = inputStream?.readBytes()
        val uploadThread = Thread {
            try {
                val uploadResult = cloudinary.uploader().upload(byteArray, ObjectUtils.emptyMap())
                val imageUrl = uploadResult["url"] as String
                firebaseHelper.saveImageUser(currentUserId, imageUrl){ successs, imagepath ->
                    if(successs){
                       callback(imagepath)
                    }else{
                        callback(null)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                callback(null) // Upload thất bại
            }
        }

        uploadThread.start()
    }


    private fun loadImage() {
        firebaseHelper.getImageUser(userId){imageUrl ->
            Glide.with(this).load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.user_default_avatar)
                .error(R.drawable.user_default_avatar)
                .into(binding.profileImageView)
        }
    }

    private fun updateUI() {
        val binding = _binding ?: return

        if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
            binding.friendActionButton.visibility = View.GONE
            binding.friendStatusTextView.visibility = View.GONE
            binding.editNameButton.visibility = View.VISIBLE
            binding.friendCountTextView .visibility = View.VISIBLE
            return
        }

        when {
            isFriend -> {
                binding.friendActionButton.text = "Hủy kết bạn"
                binding.friendActionButton.isEnabled = true
                binding.friendActionButton.visibility = View.VISIBLE
                binding.friendStatusTextView.text = "Bạn bè"
                binding.friendStatusTextView.visibility = View.VISIBLE
            }
            requestSent -> {
                binding.friendActionButton.text = "Chờ phản hồi"
                binding.friendActionButton.isEnabled = false
                binding.friendActionButton.visibility = View.VISIBLE
                binding.friendStatusTextView.visibility = View.GONE
            }
            else -> {
                binding.friendActionButton.text = "Kết bạn"
                binding.friendActionButton.isEnabled = true
                binding.friendActionButton.visibility = View.VISIBLE
                binding.friendStatusTextView.visibility = View.GONE
            }
        }
    }
    private fun showEditNamePopup() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_name_popup, null)
        val editNameEditText = dialogView.findViewById<EditText>(R.id.editNameEditText)

        builder.setView(dialogView)
            .setTitle("Đổi tên")
            .setPositiveButton("Lưu") { dialog, _ ->
                val newName = editNameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Cập nhật TextView hiển thị tên người dùng
                    binding.userNameTextView.text = newName
                    // (Tùy chọn) Cập nhật tên mới lên Firebase hoặc lưu lại thông tin người dùng
                    firebaseHelper.updateUserName(userId, newName){sucess->
                        if(sucess){
                            Toast.makeText(requireContext(), "Cập nhập tên thành công", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(requireContext(), "Cập nhập tên thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }


                } else {
                    Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

}