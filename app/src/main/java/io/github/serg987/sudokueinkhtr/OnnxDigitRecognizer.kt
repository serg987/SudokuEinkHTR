package io.github.serg987.sudokueinkhtr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.graphics.get
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections

class OnnxDigitRecognizer(context: Context) {
    private val inputSize = 28
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null

    init {
        try {
            val modelFile = ModelDownloadManager.getOnnxModelFile(context)
            if (modelFile.exists()) {
                env = OrtEnvironment.getEnvironment()
                val modelBytes = modelFile.readBytes()
                session = env?.createSession(modelBytes, OrtSession.SessionOptions())
            } else {
                Log.e("OnnxDigitRecognizer", "ONNX model file not found at ${modelFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("OnnxDigitRecognizer", "ONNX Init Error", e)
        }
    }

    class RecognitionResult(
        val digit: Int,
        val probabilities: FloatArray,
        val preprocessingTimeMs: Long,
        val inferenceTimeMs: Long
    )

    fun recognizeDigit(bitmap: Bitmap): RecognitionResult? {
        if (session == null || env == null) {
            Log.e("OnnxDigitRecognizer", "Session or Env is null. Initialization failed.")
            return null
        }

        try {
            val preStart = System.currentTimeMillis()

            // Center and crop digit
            val croppedBitmap = centerAndCropDigit(bitmap)

            // Resize to 28x28
            val scaledBitmap = croppedBitmap.scale(inputSize, inputSize, true)

            // Convert to FloatBuffer
            val floatBuffer = convertBitmapToFloatBuffer(scaledBitmap)
            val inputShape = longArrayOf(1, 1, inputSize.toLong(), inputSize.toLong())

            val preTime = System.currentTimeMillis() - preStart

            val infStart = System.currentTimeMillis()

            val inputName = session!!.inputNames.iterator().next()
            val inputTensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)
            val results = session!!.run(Collections.singletonMap(inputName, inputTensor))

            @Suppress("UNCHECKED_CAST")
            val output = results[0].value as Array<FloatArray>
            val probabilities = output[0]

            results.close()
            inputTensor.close()

            val infTime = System.currentTimeMillis() - infStart

            // Find max probability
            var maxProb = Float.NEGATIVE_INFINITY
            var maxIndex = -1
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }

            return RecognitionResult(
                digit = maxIndex,
                probabilities = probabilities,
                preprocessingTimeMs = preTime,
                inferenceTimeMs = infTime
            )

        } catch (e: Exception) {
            Log.e("OnnxDigitRecognizer", "ONNX Recognition Error", e)
            return null
        }
    }

    private fun centerAndCropDigit(bitmap: Bitmap): Bitmap {
        var minX = bitmap.width
        var maxX = 0
        var minY = bitmap.height
        var maxY = 0

        // Find digit boundaries
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3

                if (brightness > 128) { // White pixel (stroke)
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (minX >= maxX || minY >= maxY) {
            return bitmap
        }

        // Add padding (25% of size as in TF model)
        val padding = ((maxX - minX + maxY - minY) / 2 * 0.25f).toInt()
        minX = (minX - padding).coerceAtLeast(0)
        maxX = (maxX + padding).coerceAtMost(bitmap.width - 1)
        minY = (minY - padding).coerceAtLeast(0)
        maxY = (maxY + padding).coerceAtMost(bitmap.height - 1)

        val width = maxX - minX
        val height = maxY - minY

        val size = maxOf(width, height)
        val newBitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(newBitmap)
        canvas.drawColor(android.graphics.Color.BLACK)

        val offsetX = (size - width) / 2
        val offsetY = (size - height) / 2

        val croppedBitmap = Bitmap.createBitmap(bitmap, minX, minY, width, height)
        canvas.drawBitmap(croppedBitmap, offsetX.toFloat(), offsetY.toFloat(), null)

        return newBitmap
    }

    private fun convertBitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
        val floatBuffer = ByteBuffer.allocateDirect(4 * 1 * 1 * inputSize * inputSize)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = intValues[y * inputSize + x]
                val gray = ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3.0f) / 255.0f
                floatBuffer.put(gray)
            }
        }
        floatBuffer.rewind()
        return floatBuffer
    }

    fun close() {
        session?.close()
        env?.close()
    }
}
