package com.specknet.pdiotapp.authentication

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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.SelectionActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var mAuth: FirebaseAuth
    private lateinit var signUpButton: Button




    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("loginpage", "loginpage")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        // Check if the user is already logged in
        if (currentUser != null) {
            // User is already logged in, navigate to main page
            //startActivity(Intent(this, MainActivity::class.java))
            Log.d("here","here")
            //finish()
        }
        Log.d("here1", "here1")

        // Initialize UI elements
        val signInButton: Button = findViewById(R.id.loginButton)
        //  val resetText: TextView = findViewById(R.id.textView_reset)
        //  val signUp: TextView = findViewById(R.id.)
        username = findViewById(R.id.emailEditText)
        password = findViewById(R.id.passwordEditText)
        Log.d("here2", "here2")


        var user : FirebaseUser?= null
        signInButton.setOnClickListener {
            val email = username.text.toString().trim()
            val pwd = password.text.toString().trim()

            if (email.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            user = signIn(email, pwd)
            updateUI(user)
        }
        checkFirebaseConnection()

        // Sign up button listener
        signUpButton = findViewById(R.id.signUpButton)
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

    }

    private fun signIn(email: String, password: String) : FirebaseUser?{
        FirebaseApp.initializeApp(this)
      //  val database: DatabaseReference = FirebaseDatabase.getInstance("https://pdiot-j1-default-rtdb.europe-west1.firebasedatabase.app/").getReference()
        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign-in success
                    Log.d("FirebaseAuth", "signInWithEmail:success")
                    val intent = Intent(this, SelectionActivity::class.java)
                    intent.putExtra("user_email", FirebaseAuth.getInstance().toString())
                    startActivity(intent)
                    //finish()

                    // You can use `user` to access user-specific information
                } else {
                    // Sign-in failure
                    Log.e("FirebaseAuth", "signInWithEmail:failure", task.exception)
                }
            }

        val user = FirebaseAuth.getInstance().currentUser
        //updateUI(user)
        return user
    }

    private fun updateUI(user: FirebaseUser?) {
        Log.d("updateUII", user.toString())
        if (user != null) {
            // User is logged in, navigate to main page
            startActivity(Intent(this, SelectionActivity::class.java))
            finish()
        } else {
            // User not logged in, clear input fields
            username.setText("")
            password.setText("")
        }
    }

    fun checkFirebaseConnection() {
        FirebaseApp.initializeApp(this)
        val database: DatabaseReference = FirebaseDatabase.getInstance("https://pdiot-j1-default-rtdb.europe-west1.firebasedatabase.app/").getReference()

        val auth = FirebaseAuth.getInstance()
        val email = "user@pdiot.com"
        val password = "pass123"
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign-in success
                    Log.d("FirebaseAuth", "signInWithEmail:success")
                    auth.currentUser
                    // You can use `user` to access user-specific information
                } else {
                    // Sign-in failure
                    Log.e("FirebaseAuth", "signInWithEmail:failure", task.exception)
                }
            }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("FirebaseConnection", "User not authenticated. Please sign in first.")
            return
        }
        // Write a test value to check connection
        database.setValue("Testing Connection").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseConnection", "Data write successful. Connected to Firebase!")
            } else {
                Log.e("FirebaseConnection", "Failed to write data. Check your Firebase connection.")
            }
        }

        // Read back the test value to confirm
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("FirebaseConnection", "Data read successful. Connected to Firebase!")
                } else {
                    Log.e("FirebaseConnection", "Data read failed. Check your Firebase connection.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseConnection", "Error: ${error.message}")
            }
        })
    }
}