package com.specknet.pdiotapp.live


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.database.ActivityHistoryManager
import com.specknet.pdiotapp.database.ActivityLog
import com.specknet.pdiotapp.database.AppDatabase
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LiveDataActivity : AppCompatActivity() {
    private val MAX_DATA_SET_SIZE = 200


    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData
    lateinit var allThingyData: LineData

    lateinit var historyManager: ActivityHistoryManager

    lateinit var respeckChart: LineChart
    lateinit var thingyChart: LineChart

    lateinit var wakefulTextView: TextView
    lateinit var physicalTextView: TextView
    lateinit var socialTextView: TextView

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    lateinit var user_email : String

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    @Throws(IOException::class)
    private fun loadModelFile(tflite : String): MappedByteBuffer {
        val MODEL_ASSETS_PATH = tflite
        val assetFileDescrptor = this.assets.openFd(MODEL_ASSETS_PATH)
        val fileInputStream = FileInputStream(assetFileDescrptor.getFileDescriptor())
        val fileChannel = fileInputStream.getChannel()
        val startOffset = assetFileDescrptor.startOffset
        val declaredLength = assetFileDescrptor.getDeclaredLength()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    val noToString: Map<Int, String> = mapOf(
        0 to "ascending",
        1 to "descending",
        2 to "lying on back",
        3 to "lying on left",
        4 to "lying on righ",
        5 to "lying on stomach",
        6 to "misc",
        7 to "normal walking",
        8 to "running",
        9 to "shuffle walking",
        10 to "sitting / standing"
    )

    val UPDATE_FREQUENCY = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        val db = AppDatabase.getDatabase(applicationContext)
        historyManager = db.activityHistoryManager()

        user_email = intent.getStringExtra("user_email") ?: "No User"

        Log.d("THINGY LIVE", Constants.ACTION_RESPECK_LIVE_BROADCAST)
        Log.d("RESPECK LIVE", Constants.ACTION_THINGY_BROADCAST)

        socialTextView = findViewById(R.id.socialTextView)
        physicalTextView = findViewById(R.id.physicalTextView)
        wakefulTextView = findViewById(R.id.wakefulTextView)
        setupCharts()

        Log.d("Preload tensor", "got here")
        var wakefulModel : Interpreter? = Interpreter(loadModelFile("TRY_2.tflite"))
        var physicalModel : Interpreter? = Interpreter(loadModelFile("TRY_2.tflite"))
        var socialModel : Interpreter? = Interpreter(loadModelFile("TRY_2.tflite"))
        Log.d("Postload tensor", "got here")
        val modelToTextBox : Map<Interpreter?, TextView> = mapOf(
            wakefulModel to wakefulTextView,
            physicalModel to physicalTextView,
            socialModel to socialTextView
        )

        val windowSize = 50  // Adjust based on model requirements

        respeckListener(windowSize, modelToTextBox)
      //  respeckListener(windowSize, socialModel, socialTextView)
        thingyListener()


    }

    fun runModel(interpreter : Interpreter?, sensorData : Array<FloatArray>) : String{
        val sensorDataShaped = arrayOf(sensorData)
        val batch_size = sensorDataShaped.size
        val window_size = sensorDataShaped[0].size
        val channels = sensorDataShaped[0][0].size
        Log.d("tensor shape", batch_size.toString() + ", " + window_size.toString() + ", " + channels.toString())
        Log.d("Running model", "Running model")
        var outputArray =  FloatArray(11)
        interpreter!!.run(sensorDataShaped, arrayOf(outputArray))
        var out_no = (outputArray.indices.maxByOrNull {outputArray[it]} ?: -1)
        var classification = noToString[out_no] ?: "Invalid activity number"

        GlobalScope.launch(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            historyManager.insert(ActivityLog(userEmail = user_email, activity = "", timeStamp = timestamp))
        }

        return classification
    }


    fun respeckListener(windowSize : Int, modelViewMap : Map<Interpreter?, TextView>) {
        // set up the broadcast receiver
        var lastExecutionTime = 0L
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            val sensorDataBuffer = ArrayList<FloatArray>() // To store sensor readings

            override fun onReceive(context: Context, intent: Intent) {
                Log.d("receives data", "receives data")
                if (intent.action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ
                    //val gyro_y = liveData.gyro.y
                    //val gyro_z = liveData.gyro.z
                    //val gyro_x = liveData.gyro.x

                    val reading = floatArrayOf(x, y, z)

                    sensorDataBuffer.add(reading)
                    Log.d("sensorDataBuffer", sensorDataBuffer.toString())
                    if (sensorDataBuffer.size > windowSize){
                        Log.d("removing data", "removing data")
                        sensorDataBuffer.removeFirst()
                    }
                    Log.d("window size", sensorDataBuffer.size.toString())
                    if (sensorDataBuffer.size == windowSize){
                        Log.d("correct window size","correct window size")
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastExecutionTime >= UPDATE_FREQUENCY) {
                            lastExecutionTime = currentTime
                            runOnUiThread {
                                Log.d("UI thread","UI Thread")
                                modelViewMap.forEach { (model, textbox) ->
                                    var model_output = runModel(model, (sensorDataBuffer.toTypedArray()))
                                    textbox.text = model_output
                                    GlobalScope.launch(Dispatchers.IO) {
                                        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                                        val timestamp = dateFormat.format(Date())
                                        Log.d("input data", "Timestamp: " + timestamp + " Email: " + user_email + "Activity: " + model_output)

                                        try {
                                            Log.d("Data base entry", "data base entry")
                                            historyManager.insert(ActivityLog(userEmail = user_email, activity = model_output, timeStamp = timestamp))
                                            Log.d("Data base entry", "data base entry 2")
                                        } catch (e: Exception) {
                                            Log.e("DatabaseError", "Error inserting activity log", e)
                                        }

                                    }
                                }
                            }
                        }
                    }

                    time += 1
                    updateGraph("respeck", x, y, z)
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)
    }

    fun thingyListener(){
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("thingy", x, y, z)



                }
            }
        }
        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)
    }


    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        thingyChart = findViewById(R.id.thingy_chart)

        // Respeck

        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

        dataSet_res_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

        // Thingy

        time = 0f
        val entries_thingy_accel_x = ArrayList<Entry>()
        val entries_thingy_accel_y = ArrayList<Entry>()
        val entries_thingy_accel_z = ArrayList<Entry>()

        dataSet_thingy_accel_x = LineDataSet(entries_thingy_accel_x, "Accel X")
        dataSet_thingy_accel_y = LineDataSet(entries_thingy_accel_y, "Accel Y")
        dataSet_thingy_accel_z = LineDataSet(entries_thingy_accel_z, "Accel Z")

        dataSet_thingy_accel_x.setDrawCircles(false)
        dataSet_thingy_accel_y.setDrawCircles(false)
        dataSet_thingy_accel_z.setDrawCircles(false)

        dataSet_thingy_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_thingy_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_thingy_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsThingy = ArrayList<ILineDataSet>()
        dataSetsThingy.add(dataSet_thingy_accel_x)
        dataSetsThingy.add(dataSet_thingy_accel_y)
        dataSetsThingy.add(dataSet_thingy_accel_z)

        allThingyData = LineData(dataSetsThingy)
        thingyChart.data = allThingyData
        thingyChart.invalidate()
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            limitDataSetSize(dataSet_res_accel_x, MAX_DATA_SET_SIZE)
            limitDataSetSize(dataSet_res_accel_y, MAX_DATA_SET_SIZE)
            limitDataSetSize(dataSet_res_accel_z, MAX_DATA_SET_SIZE)

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        } else if (graph == "thingy") {
            dataSet_thingy_accel_x.addEntry(Entry(time, x))
            dataSet_thingy_accel_y.addEntry(Entry(time, y))
            dataSet_thingy_accel_z.addEntry(Entry(time, z))

            limitDataSetSize(dataSet_thingy_accel_x, MAX_DATA_SET_SIZE)
            limitDataSetSize(dataSet_thingy_accel_y, MAX_DATA_SET_SIZE)
            limitDataSetSize(dataSet_thingy_accel_z, MAX_DATA_SET_SIZE)

            runOnUiThread {
                allThingyData.notifyDataChanged()
                thingyChart.notifyDataSetChanged()
                thingyChart.invalidate()
                thingyChart.setVisibleXRangeMaximum(150f)
                thingyChart.moveViewToX(thingyChart.lowestVisibleX + 40)
            }
        }
    }

    private fun limitDataSetSize(dataSet: LineDataSet, maxSize: Int) {
        while (dataSet.entryCount > maxSize) {
            dataSet.removeFirst()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
    }
}
