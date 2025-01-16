package com.specknet.pdiotapp.history
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.textfield.TextInputEditText
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.database.ActivityHistoryManager
import com.specknet.pdiotapp.database.ActivityLog
import com.specknet.pdiotapp.database.AppDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class ViewHistoryActivity : AppCompatActivity() {

    private lateinit var showCalendarButton: TextInputEditText
    private lateinit var calendarView: CalendarView
    private lateinit var activitySpinner: Spinner
    private lateinit var lineChart: LineChart
    private lateinit var pieChart : PieChart
    private var calendarPopup: PopupWindow? = null
    private lateinit var database: AppDatabase
    private lateinit var activityHistoryManager: ActivityHistoryManager
    private var selectedActivity: String = ""
    private var selectedDate: String = ""

    val database_activities = listOf(
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
    val display_activities = listOf(
        "Sitting or Standing",       // Index 0
        "Lying on Left",             // Index 1
        "Lying on Right",            // Index 2
        "Lying on Back",             // Index 3
        "Lying on Stomach",          // Index 4
        "Walking",                   // Index 5
        "Running",                   // Index 6
        "Ascending Stairs",          // Index 7
        "Descending Stairs",         // Index 8
        "Shuffle Walking",           // Index 9
        "Misc"                       // Index 10
    )
    val displayToActivity = display_activities.zip(database_activities).toMap()
    val activityToDisplay = database_activities.zip(display_activities).toMap()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_history)

        // Initialize UI components
        showCalendarButton = findViewById(R.id.dateInputBox)
        activitySpinner = findViewById(R.id.activitySpinner)
        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)

        // Set up Spinner (Dropdown)
        setupSpinner()

        // Set up BarChart with sample data
        setupLineChart()
        setupPieChart(pieChart)

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
            showCalendarButton.setText(selectedDate)

            barChartData(selectedDate,selectedActivity)
            piChartData(selectedDate)

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


        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, display_activities)
        activitySpinner.adapter = adapter

        selectedActivity = display_activities[0]

        // Handle Spinner item selection
        activitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedActivity = database_activities[position]
                Log.d("SELECTED ACTIVITY", selectedActivity.toString())
                barChartData(selectedDate, selectedActivity)
                selectedActivity = activityToDisplay[selectedActivity].toString()

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

    private fun setupLineChart() {
        // Sample data for BarChart
        val entries = listOf(
            BarEntry(1f, 30f), // Example: Day 1, 30 units of activity
            BarEntry(2f, 50f), // Example: Day 2, 50 units of activity
            BarEntry(3f, 40f)  // Example: Day 3, 40 units of activity
        )

        val dataSet = LineDataSet(entries, "Activity Data")
        val lineData = LineData(dataSet)
        lineData.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 3f
        lineChart.data = lineData
        dataSet.label = activityToDisplay[dataSet.label]
        lineChart.description.text = "Time of Day"
        lineChart.description.textSize = 12f
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val hour = value.toInt() / 2 // Assuming 2 points per hour
                val minute = if (value.toInt() % 2 == 0) "00" else "30"
                return String.format("%02d:%s", hour, minute)
            }
        }
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM // Position labels at the bottom
        lineChart.xAxis.granularity = 1f // Minimum interval between labels
        lineChart.xAxis.labelCount = 6 // Display 6 labels

        lineChart.description.text = "Activity History"

        lineChart.invalidate() // Refresh the chart
    }

    private fun setupPieChart(pieChart: PieChart) {
        val pieEntries = listOf(
        com.github.mikephil.charting.data.PieEntry(40f, "Walking"),
        com.github.mikephil.charting.data.PieEntry(30f, "Running"),
        com.github.mikephil.charting.data.PieEntry(20f, "Sitting"),
        com.github.mikephil.charting.data.PieEntry(10f, "Misc")
        )

        val pieDataSet = com.github.mikephil.charting.data.PieDataSet(pieEntries, "Activity Distribution")
        pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val pieData = com.github.mikephil.charting.data.PieData(pieDataSet)

        pieChart.data = pieData
        pieChart.description.textSize = 16f
        pieChart.setCenterTextSize(18f)
        pieChart.setEntryLabelTextSize(16f)

        // Remove the legend from the PieChart
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false

        pieChart.invalidate()

        pieChart.data = pieData
        pieChart.description.text = "Activity Distribution"
        pieChart.centerText = "Activities"
        pieChart.invalidate() // Refresh the chart
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun barChartData(date: String, activity: String) {
        lifecycleScope.launch {
            val email = "user@pdiot.com"
            Log.d("DATABASE QUERY", activity)
            val display_data = activityHistoryManager.getUserActivityOnDate(email, date, activity)

            val intervalCounts = MutableList(48) { 0 }
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            for (log in display_data) {
                val dateTime = LocalDateTime.parse(log.timeStamp, formatter)
                val hour = dateTime.hour
                val minute = if (dateTime.minute >= 30) 1 else 0
                val intervalIndex = hour * 2 + minute
                intervalCounts[intervalIndex]++
            }

            // Find the peak value in intervalCounts for normalization
            val maxY = intervalCounts.maxOrNull() ?: 1 // Avoid division by zero

            // Refresh LineChart
            val lineEntries = mutableListOf<com.github.mikephil.charting.data.Entry>()
            for (i in intervalCounts.indices) {
                val normalizedValue = (intervalCounts[i].toFloat() / maxY) * 100 // Normalize to percentage
                lineEntries.add(com.github.mikephil.charting.data.Entry(i.toFloat(), normalizedValue))
            }

            val lineDataSet = LineDataSet(lineEntries, "")
            lineDataSet.setDrawValues(false)
            lineDataSet.setDrawCircles(false)
            lineDataSet.lineWidth = 3f

            lineChart.data = LineData(lineDataSet)

            // Remove legend for the line chart
            lineChart.legend.isEnabled = false
            lineChart.xAxis.textSize = 16f
            lineChart.axisLeft.textSize = 16f
            lineChart.legend.textSize = 14f
            lineChart.description.textSize = 14f

            lineChart.setExtraOffsets(0f, 0f, 0f, 8f)

            // Configure the X-axis (Time Labels)
            lineChart.xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = (value.toInt() / 2).toString().padStart(2, '0')
                        val minute = if (value.toInt() % 2 == 0) "00" else "30"
                        return "$hour:$minute"
                    }
                }
                granularity = 1f
                position = XAxis.XAxisPosition.BOTTOM
            }

            // Configure the Y-axis label
            lineChart.axisLeft.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}%" // Format as percentage
                    }
                }
                axisMinimum = 0f // Start Y-axis at 0
                axisMaximum = 100f // End Y-axis at 100%
                granularity = 10f // Label interval
            }

            // Remove right axis for clarity
            lineChart.axisRight.isEnabled = false
            lineChart.description.isEnabled = false

            lineChart.invalidate()

            // Refresh PieChart

        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun piChartData(date:String) {
        lifecycleScope.launch {
            val email = "user@pdiot.com"
            val display_data = activityHistoryManager.getAllDataOnDate(email, date)

            val activityCounts = mutableMapOf<String, Int>()
            for (log in display_data) {
                activityCounts[log.activity] = activityCounts.getOrDefault(log.activity, 0) + 1
            }

            val pieEntries = mutableListOf<com.github.mikephil.charting.data.PieEntry>()
            for ((activity, count) in activityCounts) {
                pieEntries.add(com.github.mikephil.charting.data.PieEntry(count.toFloat(), activity))
            }

            val pieDataSet = com.github.mikephil.charting.data.PieDataSet(pieEntries, "Distribution")
            pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

            pieDataSet.setDrawValues(false)

            val pieData = com.github.mikephil.charting.data.PieData(pieDataSet)

            pieChart.data = pieData
            pieChart.description.textSize = 16f
            pieChart.setCenterTextSize(18f)
            pieChart.setEntryLabelTextSize(16f)

            // Remove the legend from the PieChart
            pieChart.legend.isEnabled = false
            pieChart.description.isEnabled = false

            pieChart.invalidate()
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

                // Calculate the time fraction relative to the total duration
                val timeFraction = elapsedSeconds.toDouble() / totalDurationSeconds.toDouble()

                // Shift the distribution to peak around 75% of the duration
                val targetFraction = 1 // Peak at 75%
                val distanceFromTarget = kotlin.math.abs(timeFraction - targetFraction)
                val probability = 1 - distanceFromTarget // Higher probability near the targetFraction

                // Ensure the probability is in the range [0, 1]
                val normalizedProbability = probability.coerceIn(0.0, 1.0)

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