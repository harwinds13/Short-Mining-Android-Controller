package com.harry.shortmining

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var nameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        nameEdit = findViewById(R.id.nameEdit)
        emailEdit = findViewById(R.id.emailEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        phoneEdit = findViewById(R.id.phoneEdit)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.loginTextView)
        progressBar = findViewById(R.id.progressBar)

        // Set click listener for register button
        registerButton.setOnClickListener {
            registerUser()
        }

        // Set click listener for login text
        loginTextView.setOnClickListener {
            // Navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = nameEdit.text.toString().trim()
        val email = emailEdit.text.toString().trim()
        val password = passwordEdit.text.toString().trim()
        val phone = phoneEdit.text.toString().trim()

        // Validate input fields
        if (name.isEmpty()) {
            nameEdit.error = "Name is required"
            nameEdit.requestFocus()
            return
        }

        if (email.isEmpty()) {
            emailEdit.error = "Email is required"
            emailEdit.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEdit.error = "Password is required"
            passwordEdit.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordEdit.error = "Password should be at least 6 characters"
            passwordEdit.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            phoneEdit.error = "Phone number is required"
            phoneEdit.requestFocus()
            return
        }

        // Show progress bar
        progressBar.visibility = View.VISIBLE

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success, update UI with the signed-in user's information
                    val user = auth.currentUser

                    // Get current UTC time in YYYY-MM-DD HH:MM:SS format
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val currentDateAndTime = sdf.format(Date())

                    // Create user document in Firestore
                    val db = Firebase.firestore
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "status" to "inactive",
                        "createdAt" to currentDateAndTime,
                        "updatedAt" to currentDateAndTime
                    )

                    user?.let {
                        db.collection("users").document(it.uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

                                // Navigate to MainActivity
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("USER_ID", user.uid)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // If sign up fails, display a message to the user
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}