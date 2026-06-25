package io.github.serg987.sudokueinkhtr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale
import androidx.core.graphics.get
import androidx.core.graphics.createBitmap

class DigitRecognizer(context: Context) {
    private var interpreter: Interpreter
    private val inputSize = 28

    init {
        val model = loadModelFile(context)
        interpreter = Interpreter(model)

        // DEBUG: Veure dimensions
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        println("Input shape: ${inputTensor.shape().contentToString()}")
        println("Input type: ${inputTensor.dataType()}")
        println("Output shape: ${outputTensor.shape().contentToString()}")
        println("Output type: ${outputTensor.dataType()}")
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("mnist.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun recognizeDigit(bitmap: Bitmap): Int {
        try {
            // Centrar i retallar el dígit
            val croppedBitmap = centerAndCropDigit(bitmap)

            // Redimensionar a 28x28
            val scaledBitmap = croppedBitmap.scale(inputSize, inputSize, true)

            // Convertir a ByteBuffer
            val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)

            // Array de sortida (10 probabilitats per 0-9)
            val result = Array(1) { FloatArray(10) }

            // Executar inferència
            interpreter.run(byteBuffer, result)

            // DEBUG: Mostrar top 3 prediccions
            val sorted = result[0].indices.sortedByDescending { result[0][it] }
            println("Top 3: ${sorted[0]}(${result[0][sorted[0]]}), ${sorted[1]}(${result[0][sorted[1]]}), ${sorted[2]}(${result[0][sorted[2]]})")

            // Retornar el dígit amb més probabilitat
            return sorted[0]

        } catch (e: Exception) {
            println("ERROR reconeixement: ${e.message}")
            e.printStackTrace()
            return 0
        }
    }

    private fun centerAndCropDigit(bitmap: Bitmap): Bitmap {
        var minX = bitmap.width
        var maxX = 0
        var minY = bitmap.height
        var maxY = 0

        // Trobar els límits del dígit
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3

                if (brightness > 128) { // Píxel blanc (traç)
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // Si no s'ha trobat cap píxel, retornar el bitmap original
        if (minX >= maxX || minY >= maxY) {
            return bitmap
        }

        // Afegir padding (20% del tamany)
        val padding = ((maxX - minX + maxY - minY) / 2 * 0.25f).toInt()
        minX = (minX - padding).coerceAtLeast(0)
        maxX = (maxX + padding).coerceAtMost(bitmap.width - 1)
        minY = (minY - padding).coerceAtLeast(0)
        maxY = (maxY + padding).coerceAtMost(bitmap.height - 1)

        val width = maxX - minX
        val height = maxY - minY

        // Fer-lo quadrat
        val size = maxOf(width, height)
        val newBitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(newBitmap)
        canvas.drawColor(android.graphics.Color.BLACK)

        val offsetX = (size - width) / 2
        val offsetY = (size - height) / 2

        val croppedBitmap = Bitmap.createBitmap(bitmap, minX, minY, width, height)  // ← Així
        canvas.drawBitmap(croppedBitmap, offsetX.toFloat(), offsetY.toFloat(), null)

        return newBitmap
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // El model espera UINT8 (0-255), no FLOAT32
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixel = intValues[i * inputSize + j]
                // Convertir a escala de grisos 0-255 (UINT8)
                val gray = ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3).toByte()
                byteBuffer.put(gray)
            }
        }

        return byteBuffer
    }


    fun close() {
        interpreter.close()
    }
}
