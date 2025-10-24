package com.harry.shortmining

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var statusListener: ListenerRegistration? = null
    private lateinit var buttonEnableService: Button
    private lateinit var buttonClientViewActivity: Button
    private var userId: String? = null
    private lateinit var db: FirebaseFirestore

    private lateinit var tvVendorName: TextView
    private lateinit var tvVendorEmail: TextView
    private lateinit var tvVendorPhone: TextView
    private lateinit var tvVendorCompany: TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvVendorName = findViewById(R.id.tvVendorName)
        tvVendorEmail = findViewById(R.id.tvVendorEmail)
        tvVendorPhone = findViewById(R.id.tvVendorPhone)
        tvVendorCompany = findViewById(R.id.tvVendorCompany)

        tvVendorName.text = "Loading..."
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        } else {
            setContentView(R.layout.activity_main)
        }

        setupStatusListener(userId!!)


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        buttonEnableService = findViewById(R.id.button)
        buttonClientViewActivity = findViewById(R.id.buttonToList)
        loadVendorProfile(userId!!)
        buttonEnableService.setOnClickListener {
            val intent = Intent(this, WebView::class.java)
            intent.putExtra("url", "https://hiring.amazon.ca")
            startActivity(intent)
        }
        buttonClientViewActivity.setOnClickListener {
            startActivity(Intent(this, ClientSheetView::class.java))
        }
    }

    private fun loadVendorProfile(userId: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    var name = document.getString("name")?: "N/A"
                    Toast.makeText(this, "User profile loaded."+name, Toast.LENGTH_SHORT).show()
                    tvVendorName.text = name
                    tvVendorEmail.text = document.getString("email") ?: currentUser.email ?: "N/A"
                    tvVendorPhone.text = document.getString("phone") ?: "N/A"
                    tvVendorCompany.text = document.getString("company") ?: "N/A"
                } else {
                    Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    // If user document doesn't exist, show email from auth
                    tvVendorEmail.text = currentUser.email ?: "N/A"
                    tvVendorName.text = currentUser.displayName ?: "Vendor"
                    tvVendorPhone.text = "N/A"
                    tvVendorCompany.text = "N/A"
                }
            }
            .addOnFailureListener { e ->
                // Show default values on failure
                tvVendorEmail.text = currentUser.email ?: "N/A"
                tvVendorName.text = "Vendor"
                tvVendorPhone.text = "N/A"
                tvVendorCompany.text = "N/A"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupStatusListener(userId: String) {
        val db = Firebase.firestore

        statusListener = db.collection("users").document(userId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val status = documentSnapshot.getString("status")
                    if (status != "active") {
                        auth.signOut()
                        goToLogin()
                    }
                } else {
                    auth.signOut()
                    goToLogin()
                }
            }
    }

}