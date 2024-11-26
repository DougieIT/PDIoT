import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.IOException

class ModelInterpreter(private val context: Context) {

    // Declare a TFLite Interpreter
    private var interpreter: Interpreter? = null

    init {
        interpreter = Interpreter(loadModelFile("../assets/model.tflite"))
    }

    // Load the .tflite model file from assets
    private fun loadModelFile(modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Example function to run inference
    fun runInference(inputData: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4).order(ByteOrder.nativeOrder())
        for (value in inputData) {
            inputBuffer.putFloat(value)
        }

        // Define the output buffer for the model
        val outputData = FloatArray(10) // adjust this size according to your model’s output shape
        interpreter?.run(inputBuffer, outputData)
        
        return outputData
    }

    // Close the interpreter when done
    fun close() {
        interpreter?.close()
    }
}