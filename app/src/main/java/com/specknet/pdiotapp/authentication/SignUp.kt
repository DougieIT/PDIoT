package com.specknet.pdiotapp.database

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.specknet.pdiotapp.MainActivity
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.onboarding.OnBoardingActivity

class SignUpActivity : AppCompatActivity() {
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var signUpButton: Button
    private lateinit var cancelButton: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        mAuth = FirebaseAuth.getInstance()
        FirebaseApp.initializeApp(this)

        // Initialize UI elements
        emailField = findViewById(R.id.emailTextBox)
        passwordField = findViewById(R.id.passwordTextBox)
        confirmPasswordField = findViewById(R.id.confirmPasswordTextBox)
        signUpButton = findViewById(R.id.signUpBtn)
        cancelButton = findViewById(R.id.cancelButton)

        // Handle sign-up button click
        signUpButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createUser(email, password)
        }
        cancelButton.setOnClickListener {
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }
    }

    private fun createUser(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Registration success
                    val user = mAuth.currentUser
                    user?.let {
                        saveUserToDatabase(it)
                    }
                    updateUI(user)
                } else {
                    // Registration failure
                    Log.e("FirebaseAuth", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(user: FirebaseUser) {
        val database = FirebaseDatabase.getInstance("https://pdiot-j1-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val userId = user.uid
        val userInfo = mapOf("email" to user.email)

        database.child("users").child(userId).setValue(userInfo)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseDatabase", "User data saved successfully")
                    val mainIntent = Intent(this, MainActivity::class.java)
                    startActivity(mainIntent)
                } else {
                    Log.e("FirebaseDatabase", "Failed to save user data", task.exception)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}