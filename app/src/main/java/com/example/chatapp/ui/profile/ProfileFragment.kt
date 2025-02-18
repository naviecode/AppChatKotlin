package com.example.chatapp.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.chatapp.databinding.FragmentProfileBinding
import com.example.chatapp.utils.FirebaseHelper
import com.example.chatapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance().collection("users")
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

        // Tải ảnh đại diện nếu có
        loadProfileImage()

        // Chọn ảnh từ thư viện
        binding.profileImageView.setOnClickListener {
            openGallery()
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let { saveImageToInternalStorage(it) }  // Lưu vào Internal Storage
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        val fileName = "profile_image_$userId.jpg"

        val file = File(requireContext().filesDir, fileName)

        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            saveImagePath(file.absolutePath) // Lưu đường dẫn vào Firestore
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Lỗi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImagePath(filePath: String) {

        userId?.let {
            firestore.document(it).update("profileImagePath", filePath)
                .addOnSuccessListener {
                    Log.d("FirestoreUpdate", "Ảnh cập nhật thành công!")
                    Toast.makeText(requireContext(), "Ảnh cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    loadImage(filePath)
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreUpdate", "Lỗi lưu đường dẫn: ${e.message}")
                    Toast.makeText(requireContext(), "Lỗi lưu đường dẫn: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadProfileImage() {
        userId?.let {
            firestore.document(it).get().addOnSuccessListener { document ->
                document.getString("profileImagePath")?.let { path ->
                    loadImage(path)
                }
            }
        }
    }

    private fun loadImage(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            Glide.with(this).load(file).into(binding.profileImageView)
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateUI() {
        if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
            binding.friendActionButton.visibility = View.GONE
            binding.friendStatusTextView.visibility = View.GONE
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
}