package com.harry.shortmining

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var loginButton: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        auth = FirebaseAuth.getInstance()

        emailEdit = findViewById(R.id.emailEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        loginButton = findViewById(R.id.loginButton)

        // Inside LoginActivity.kt, add this in onCreate() method
        val registerTextView: TextView = findViewById(R.id.registerTextView)
        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            checkUserActiveStatus(userId)
                        } else {
                            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "wrong credentials", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    }

    private fun checkUserActiveStatus(userId: String) {
        val db = Firebase.firestore

        // Get the user document from Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Check the 'status' field in the user document
                    val status = document.getString("status")

                    if (status == "active") {
                        // User is active, proceed to main activity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // User is not active
                        Toast.makeText(this, "USER Not Eligible. Please contact support.", Toast.LENGTH_LONG).show()
                        // Sign out the user since they shouldn't remain authenticated
                        auth.signOut()
                    }
                } else {
                    // User document doesn't exist
                    Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                // Error getting the document
                Toast.makeText(this, "Error checking account status: ${e.message}", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

}