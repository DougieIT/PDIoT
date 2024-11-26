package com.specknet.pdiotapp.history


import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CalendarView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.specknet.pdiotapp.R
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.specknet.pdiotapp.database.ActivityHistoryManager
import com.specknet.pdiotapp.database.ActivityLog
import com.specknet.pdiotapp.database.AppDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ViewHistoryActivity : AppCompatActivity() {

    private lateinit var showCalendarButton: Button
    private lateinit var calendarView: CalendarView
    private lateinit var activitySpinner: Spinner
    private lateinit var barChart: BarChart
    private var calendarPopup: PopupWindow? = null
    private lateinit var database: AppDatabase
    private lateinit var activityHistoryManager: ActivityHistoryManager
    private var selectedActivity: String = ""
    private var selectedDate: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_history)

        // Initialize UI components
        showCalendarButton = findViewById(R.id.showCalendarButton)
        activitySpinner = findViewById(R.id.activitySpinner)
        barChart = findViewById(R.id.barChart)

        // Set up Spinner (Dropdown)
        setupSpinner()

        // Set up BarChart with sample data
        setupBarChart()

        database = AppDatabase.getDatabase(this)
        activityHistoryManager = database.activityHistoryManager()

        // Handle calendar visibility toggle
        showCalendarButton.setOnClickListener {
            if (calendarView.visibility == View.GONE) {
                Log.d("calendar", "calendar visible")
                calendarView.visibility = View.VISIBLE
            } else {
                Log.d("calendar", "calendar invisible")
                calendarView.visibility = View.GONE
            }
        }

        showCalendarButton.setOnClickListener {
            showCalendarPopup()
        }
        generateSyntheticDataWithTrend()
        insertSampleData()
    }

    private fun insertSampleData() {
        lifecycleScope.launch {
            try {
                activityHistoryManager.insertSampleData()
                Log.d("Database", "Sample data inserted successfully")
                Toast.makeText(this@ViewHistoryActivity, "Sample data inserted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Database", "Error inserting sample data", e)
                Toast.makeText(this@ViewHistoryActivity, "Error inserting sample data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCalendarPopup() {
        // Inflate the popup layout
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_calendar, null)

        // Initialize the CalendarView in the popup
        val calendarView: CalendarView = popupView.findViewById(R.id.popupCalendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            barChartData(selectedDate,selectedActivity)
            Toast.makeText(this, "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()
            calendarPopup?.dismiss()
        }

        // Create the PopupWindow
        calendarPopup = PopupWindow(
            popupView,
            resources.getDimensionPixelSize(R.dimen.popup_width),
            resources.getDimensionPixelSize(R.dimen.popup_height),
            true // Focusable
        )

        // Show the PopupWindow
        calendarPopup?.showAtLocation(findViewById(android.R.id.content), android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, showCalendarButton.bottom)

    }

    private fun setupSpinner() {
        // List of activities for the dropdown
        val activities = listOf(
            "sitting/ Standing",       // Index 0
            "lying down on left",      // Index 1
            "lying down on right",     // Index 2
            "lying down on back",      // Index 3
            "lying down on stomach",   // Index 4
            "walking",                 // Index 5
            "running",                 // Index 6
            "ascending stairs",        // Index 7
            "descending stairs",       // Index 8
            "shuffle walking",         // Index 9
            "misc"                     // Index 10
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activities)
        activitySpinner.adapter = adapter

        selectedActivity = activities[0]

        // Handle Spinner item selection
        activitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedActivity = activities[position]
                barChartData(selectedDate,selectedActivity)

                Toast.makeText(
                    this@ViewHistoryActivity,
                    "Selected Activity: $selectedActivity",
                    Toast.LENGTH_SHORT
                ).show()
                // Perform other actions based on selected activity, such as updating the chart
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when no selection is made, if needed
            }
        }
    }

    private fun setupBarChart() {
        // Sample data for BarChart
        val entries = listOf(
            BarEntry(1f, 30f), // Example: Day 1, 30 units of activity
            BarEntry(2f, 50f), // Example: Day 2, 50 units of activity
            BarEntry(3f, 40f)  // Example: Day 3, 40 units of activity
        )

        val dataSet = BarDataSet(entries, "Activity Data")
        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.description.text = "Activity History"
        barChart.invalidate() // Refresh the chart
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun barChartData(date : String, activity : String){
        lifecycleScope.launch {
            val email = "user@pdiot.com"
            val display_data = activityHistoryManager.getUserActivityOnDate(email, date, activity)
            Log.d("Database query", email +  ", " + date + ", " + activity)
            Log.d("Database display_data",display_data.toString())
            val intervalCounts = MutableList(48) { 0 }

// 2. Define the date formatter
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

// 3. Process each ActivityLog entry
            for (log in display_data) {
                val dateTime = LocalDateTime.parse(log.timeStamp, formatter)
                val hour = dateTime.hour
                val minute = dateTime.minute

                val intervalIndex = hour * 2 + if (minute >= 30) 1 else 0
                intervalCounts[intervalIndex] += 1
            }

// 4. Create BarEntry objects
            val entries = mutableListOf<BarEntry>()
            for (i in intervalCounts.indices) {
                val count = intervalCounts[i]
                entries.add(BarEntry(i.toFloat(), count.toFloat()))
            }

// 5. Create BarDataSet
            val dataSet = BarDataSet(entries, activity)
            dataSet.setDrawValues(false)

// 6. Configure the BarDataSet (optional)


// 7. Create BarData and set it to the chart
            val barData = BarData(dataSet)

// Adjust the bar width
            barData.barWidth = 0.9f // make the bar wider

// 8. Set up the BarChart view
            val barChart = findViewById<BarChart>(R.id.barChart)
            barChart.data = barData
            barChart.setFitBars(true) // make the x-axis fit exactly all bars

// 9. Create labels for x-axis
            val intervalLabels = mutableListOf<String>()
            for (i in 0 until 48) {
                val hour = i / 2
                val minute = if (i % 2 == 0) "00" else "30"
                intervalLabels.add(String.format("%02d:%s", hour, minute))
            }

// 10. Configure x-axis labels
            val xAxis = barChart.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(intervalLabels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 12 // Adjust for label density

// Optional: Configure other chart properties
            barChart.axisRight.isEnabled = false
            barChart.description.isEnabled = false
            barChart.legend.isEnabled = true

// 11. Refresh the chart
            barChart.invalidate()
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateSyntheticDataWithTrend() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val activity = "sitting/ Standing"

        // Define the date (17th of the current month and year)
        val currentDate = LocalDate.now()
        val date = LocalDate.of(currentDate.year, currentDate.month, 17)

        // Start and end times for the data generation (9 AM to 5 PM)
        val startTime = LocalDateTime.of(date, LocalTime.of(9, 0))  // 09:00 AM
        val endTime = LocalDateTime.of(date, LocalTime.of(17, 0))   // 05:00 PM

        // Measurement interval in seconds
        val intervalSeconds = 10L // Every 10 seconds

        // Total duration in seconds
        val totalDurationSeconds = java.time.Duration.between(startTime, endTime).seconds

        // List to hold timestamps
        val timestamps = mutableListOf<LocalDateTime>()
        var currentTimestamp = startTime

        // Generate timestamps from 9 AM to 5 PM at 10-second intervals
        while (currentTimestamp.isBefore(endTime) || currentTimestamp.isEqual(endTime)) {
            timestamps.add(currentTimestamp)
            currentTimestamp = currentTimestamp.plusSeconds(intervalSeconds)
        }

        // Insert data into the database
        lifecycleScope.launch {
            val dataToInsert = mutableListOf<ActivityLog>()

            for (timestamp in timestamps) {
                // Calculate the time elapsed since start time in seconds
                val elapsedSeconds = java.time.Duration.between(startTime, timestamp).seconds

                // Calculate the probability using a sine function
                val timeFraction = elapsedSeconds.toDouble() / totalDurationSeconds.toDouble()
                val probability = sin(PI * timeFraction)

                // Normalize probability to [0,1] range
                val normalizedProbability = (probability + 0) / 1.0  // Since sin(0 to PI) ranges from 0 to 1

                // Introduce randomness
                val randomValue = Random.nextFloat()

                if (randomValue <= normalizedProbability) {
                    // Create ActivityLog entry
                    val activityLog = ActivityLog(
                        userEmail = "user@pdiot.com",
                        timeStamp = timestamp.format(dateFormatter),
                        activity = activity
                    )
                    dataToInsert.add(activityLog)
                }
            }

            // Insert data into the database
            try {
                activityHistoryManager.insertAll(dataToInsert)
                Log.d("SyntheticData", "Inserted ${dataToInsert.size} synthetic activity logs with trend.")
                Toast.makeText(this@ViewHistoryActivity, "Synthetic data with trend inserted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SyntheticData", "Error inserting synthetic data", e)
                Toast.makeText(this@ViewHistoryActivity, "Error inserting synthetic data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}