package com.specknet.pdiotapp.live

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.R

class ThingySelectActivity : AppCompatActivity() {

    private lateinit var respeckOnlyButton: Button
    private lateinit var respeckAndThingyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thingy_select)

        // Initialize buttons
        respeckOnlyButton = findViewById(R.id.respeckOnly)
        respeckAndThingyButton = findViewById(R.id.respeckAndThingy)

        // Set click listeners for each button
        respeckOnlyButton.setOnClickListener {
            Toast.makeText(this, "Button 1 clicked", Toast.LENGTH_SHORT).show()
            // Add actions for Button 1 click here, e.g., navigate to another activity
            // val intent = Intent(this, SomeOtherActivity::class.java)
            // startActivity(intent)
        }

        respeckAndThingyButton.setOnClickListener {
            Toast.makeText(this, "Button 2 clicked", Toast.LENGTH_SHORT).show()
            // Add actions for Button 2 click here, e.g., navigate to another activity
            // val intent = Intent(this, AnotherActivity::class.java)
            // startActivity(intent)
        }
    }
}