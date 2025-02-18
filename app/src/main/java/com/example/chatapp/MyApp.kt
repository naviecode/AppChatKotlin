package com.example.chatapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Kiểm tra kết nối Firebase Authentication
        checkFirebaseAuth()

        // Kiểm tra kết nối Firestore
        checkFirestoreConnection()

        // Kiểm tra kết nối RealTime Database
        checkRealtimeDatabaseConnection()

    }

    private fun checkFirebaseAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth != null) {
            Log.d("FirebaseCheck", "FirebaseAuth đã khởi tạo thành công")
        } else {
            Log.e("FirebaseCheck", "FirebaseAuth KHÔNG được khởi tạo")
        }
    }

    private fun checkFirestoreConnection() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener {
                Log.d("FirebaseCheck", "Firestore kết nối thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseCheck", "Firestore lỗi kết nối: ${e.message}")
            }
    }

    private fun checkRealtimeDatabaseConnection() {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference(".info/connected")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d("FirebaseCheck", "Realtime Database kết nối thành công!")
                } else {
                    Log.e("FirebaseCheck", "Realtime Database KHÔNG kết nối được!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseCheck", "Lỗi Realtime Database: ${error.message}")
            }
        })
    }



}