package com.specknet.pdiotapp.live

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.live.LiveDataActivity.NormalizationParams
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.Duration
import java.time.LocalDateTime

class SleepClassification : AppCompatActivity() {

    lateinit var wakefulModel : Interpreter
    lateinit var turningModel : Interpreter
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    val UPDATE_FREQUENCY = 2000 // in milliseconds
    private val normalizationParamsTRY2 = NormalizationParams(
        mean = floatArrayOf(-0.03325532f, -0.59998163f, 0.03538302f),
        std = floatArrayOf(0.45624453f, 0.54131043f, 0.51403646f)
    )

    lateinit var wakefulTextView: TextView
    lateinit var turnsTextView: TextView
    lateinit var efficiencyTextView: TextView
    lateinit var startRecordButton : Button
    lateinit var qualityTextView: TextView

    lateinit var startedRecording : LocalDateTime

    var lastPosition = "N/A"
    var turns = 0

    var sleepingReadings = 0

    var recording : Boolean = false

    val activitiesMap: Map<Int, String> = mapOf(
        0 to "ascending",
        1 to "descending",
        2 to "lying on back",
        3 to "lying on left",
        4 to "lying on right",
        5 to "lying on stomach",
        6 to "misc",
        7 to "normal walking",
        8 to "running",
        9 to "shuffle walking",
        10 to "sitting / standing"
    )


    val turnPositionMap: Map<Int, String> = mapOf(

        0 to "lying on back",
        1 to "lying on left",
        2 to "lying on stomach",
        3 to "lying on right",

    )


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the layout for this activity
        setContentView(R.layout.activity_sleep)

        wakefulTextView = findViewById(R.id.wakefulTextView)
        turnsTextView = findViewById(R.id.turnsTextView)
        efficiencyTextView = findViewById(R.id.efficiencyTextView)
        startRecordButton = findViewById(R.id.startRecordingButton)

        try {
            wakefulModel = Interpreter(loadModelFile("TRY_2.tflite"))
            turningModel = Interpreter(loadModelFile("TRY_2.tflite"))
        } catch (e: IOException) {
            Log.e("ModelError", "Error loading models", e)
        }

        startRecordButton.setOnClickListener {
            Log.d("RECORDING: ", recording.toString())
            if (recording) {
                startRecordButton.text = "Start Recording"
                startRecordButton.setBackgroundColor(ContextCompat.getColor(this, R.color.green_theme))
                wakefulTextView.text = "N/A"
                turnsTextView.text = "0"
                efficiencyTextView.text = "N/A"
                recording = false
            } else {
                startedRecording = LocalDateTime.now()
                startRecordButton.setText("Stop Recording")
                startRecordButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                lastPosition = "N/A"
                turns = 0
                sleepingReadings = 0
                recording = true
            }
        }
        respeckListener(windowSize=50)
    }

    @Throws(IOException::class)
    private fun loadModelFile(tflite: String): MappedByteBuffer {
        val MODEL_ASSETS_PATH = tflite
        val assetFileDescriptor = this.assets.openFd(MODEL_ASSETS_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun runModel(
        interpreter: Interpreter,
        sensorData: Array<FloatArray>,
        params: NormalizationParams,
        labels : Map<Int, String>
    ): String {
        // Normalize the sensor data
        val normalizedData = sensorData.map { (x, y, z) ->
            normalize(x, y, z, params)
        }.toTypedArray()

        val sensorDataShaped = arrayOf(normalizedData) // Shape: [1, window_size, 3]

        Log.d("tensor shape", "${sensorDataShaped.size}, ${sensorDataShaped[0].size}, ${sensorDataShaped[0][0].size}")
        Log.d("Running model", "Running model")

        val outputArray = Array(1) { FloatArray(11) } // Adjusted to match [1, 11]
        interpreter.run(sensorDataShaped, outputArray)

        val outNo = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1
        val classification = labels[outNo] ?: "Invalid activity number"

        return classification
    }

    private fun normalize(
        x: Float,
        y: Float,
        z: Float,
        params: NormalizationParams
    ): FloatArray {
        val normalizedX = (x - params.mean[0]) / params.std[0]
        val normalizedY = (y - params.mean[1]) / params.std[1]
        val normalizedZ = (z - params.mean[2]) / params.std[2]
        return floatArrayOf(normalizedX, normalizedY, normalizedZ)
    }

    fun respeckListener(windowSize: Int) {
        // Set up the broadcast receiver
        if (recording) {
            var lastExecutionTime = 0L
            respeckLiveUpdateReceiver = object : BroadcastReceiver() {
                val sensorDataBuffer = ArrayList<FloatArray>() // To store raw sensor readings

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d("receives data", "receives data")
                    if (intent.action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                        val liveData =
                            intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData

                        // Get all relevant intent contents
                        val x = liveData.accelX
                        val y = liveData.accelY
                        val z = liveData.accelZ

                        // Add raw data to the buffer
                        sensorDataBuffer.add(floatArrayOf(x, y, z))
                        Log.d("sensorDataBuffer", sensorDataBuffer.toString())
                        if (sensorDataBuffer.size > windowSize) {
                            Log.d("removing data", "removing data")
                            sensorDataBuffer.removeAt(0)
                        }
                        Log.d("window size", sensorDataBuffer.size.toString())
                        if (sensorDataBuffer.size == windowSize) {
                            Log.d("correct window size", "correct window size")
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastExecutionTime >= UPDATE_FREQUENCY) {
                                lastExecutionTime = currentTime
                                runOnUiThread {
                                    Log.d("UI thread", "UI Thread")
                                    // Prepare the input data
                                    val inputData =
                                        sensorDataBuffer.toTypedArray() // Shape: [window_size, 3]


                                    // Wakefulness
                                    val wakefulModelOutput = runModel(
                                        wakefulModel,
                                        arrayOf(*inputData),
                                        normalizationParamsTRY2, activitiesMap
                                    )
                                    wakefulTextView.text = wakefulModelOutput

                                    // Position Changes
                                    val turnModelOutput = runModel(
                                        turningModel,
                                        arrayOf(*inputData),
                                        normalizationParamsTRY2,
                                        activitiesMap
                                    )  // need new params here
                                    if (turnModelOutput != lastPosition) {
                                        turns = turns + 1
                                        turnsTextView.text = turns.toString()
                                    }
                                    lastPosition = turnModelOutput

                                    // Efficiency
                                    var totalTimeMeasured = Duration.between(startedRecording, LocalDateTime.now())
                                    var secondsBetween = totalTimeMeasured.seconds
                                    var readings = secondsBetween/2
                                    if (wakefulModelOutput == "Awake"){
                                        sleepingReadings += 1
                                    }


                                    val sleep_effiency = sleepingReadings / readings
                                    efficiencyTextView.text = (100 * sleep_effiency).toString()

                                    // Quality Index
                                    val idealSleepDuration = 8 * 60 * 60

                                    val durationFactor = 1 - Math.abs(secondsBetween - idealSleepDuration).toDouble() / idealSleepDuration

                                    val maxExpectedTurns = 40
                                    val turnFactor = 1 - Math.min(turns.toDouble() / maxExpectedTurns, 1.0)

                                    val w1 = 0.4 // Weight for efficiency
                                    val w2 = 0.3 // Weight for duration factor
                                    val w3 = 0.3 // Weight for turn factor

                                    val sqi = (w1 * (sleep_effiency) + (w2 * durationFactor) + (w3 * turnFactor))
                                    qualityTextView.text = sqi.toString()



                                }
                            }
                        }
                    }
                }
            }
        }

    }


}