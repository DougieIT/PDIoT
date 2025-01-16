package com.specknet.pdiotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.history.ViewHistoryActivity
import com.specknet.pdiotapp.live.LiveDataActivity
import com.specknet.pdiotapp.live.SleepClassification
import com.specknet.pdiotapp.onboarding.OnBoardingActivity

class SelectionActivity : AppCompatActivity() {

    // UI elements
    lateinit var liveProcessingButton: CardView
    lateinit var pairingButton: CardView
    lateinit var viewHistoryButton : CardView
    lateinit var sleepEfficiencyButton : CardView
    lateinit var permissionAlertDialog: AlertDialog.Builder

    lateinit var user_email : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)
        Log.d("UI FLOW:", "LIVE DATA")

        Log.d("Selection activity", "fdbfgfvgft4")
        user_email = intent.getStringExtra("user_email").toString()




        liveProcessingButton = findViewById(R.id.live_card)
        pairingButton = findViewById(R.id.ble_card)
        viewHistoryButton = findViewById(R.id.history_card)
        sleepEfficiencyButton = findViewById(R.id.sleep_card)

        permissionAlertDialog = AlertDialog.Builder(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        liveProcessingButton.setOnClickListener {
            val intent = Intent(this, LiveDataActivity::class.java)
            intent.putExtra("user_email", user_email)
            startActivity(intent)
        }

        sleepEfficiencyButton.setOnClickListener {
            val intent = Intent(this, SleepClassification::class.java)
            intent.putExtra("user_email", user_email)
            startActivity(intent)
        }

        pairingButton.setOnClickListener {
            val intent = Intent(this, ConnectingActivity::class.java)
            startActivity(intent)
        }

        viewHistoryButton.setOnClickListener {
            val intent =Intent(this, ViewHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        finishAffinity() // Exits the app completely
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.show_tutorial) {
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}