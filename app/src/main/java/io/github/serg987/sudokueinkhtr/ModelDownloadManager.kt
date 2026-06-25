package io.github.serg987.sudokueinkhtr

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloadManager {

    private const val ONNX_MODEL_URL = "https://huggingface.co/deepshah23/digit-blank-classifier-cnn/resolve/main/mnist_emnist_blank_cnn_v1.onnx"
    private const val ONNX_FILE_NAME = "mnist_emnist_blank_cnn_v1.onnx"

    // State flow to track ONNX download progress (0.0 to 1.0, or -1.0 for error)
    private val _onnxDownloadProgress = MutableStateFlow<Float?>(null)
    val onnxDownloadProgress: StateFlow<Float?> = _onnxDownloadProgress

    private val _mlKitDownloadState = MutableStateFlow<Boolean?>(null) // true if downloading, false if error, null if idle
    val mlKitDownloadState: StateFlow<Boolean?> = _mlKitDownloadState

    fun getOnnxModelFile(context: Context): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return File(modelsDir, ONNX_FILE_NAME)
    }

    fun isOnnxModelDownloaded(context: Context): Boolean {
        val file = getOnnxModelFile(context)
        return file.exists() && file.length() > 3 * 1024 * 1024 // > 3MB
    }

    suspend fun downloadOnnxModel(context: Context): Boolean = withContext(Dispatchers.IO) {
        _onnxDownloadProgress.value = 0f
        var success = false
        val file = getOnnxModelFile(context)
        val tempFile = File(file.parent, "$ONNX_FILE_NAME.tmp")
        
        try {
            val url = URL(ONNX_MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                _onnxDownloadProgress.value = -1f
                return@withContext false
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(tempFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toFloat() / 100f
                    _onnxDownloadProgress.value = progress
                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()

            if (tempFile.length() > 3 * 1024 * 1024) {
                tempFile.renameTo(file)
                success = true
            } else {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("ModelDownloadManager", "Error downloading ONNX model", e)
            tempFile.delete()
        } finally {
            if (!success) {
                _onnxDownloadProgress.value = -1f
            } else {
                _onnxDownloadProgress.value = 1f
            }
        }
        return@withContext success
    }

    fun isMlKitModelDownloaded(onResult: (Boolean) -> Unit) {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            if (modelIdentifier != null) {
                val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                val remoteModelManager = RemoteModelManager.getInstance()
                remoteModelManager.isModelDownloaded(model)
                    .addOnSuccessListener { isDownloaded ->
                        onResult(isDownloaded)
                    }
                    .addOnFailureListener {
                        onResult(false)
                    }
            } else {
                onResult(false)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun downloadMlKitModel(onComplete: (Boolean) -> Unit) {
        _mlKitDownloadState.value = true
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            if (modelIdentifier != null) {
                val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                val remoteModelManager = RemoteModelManager.getInstance()
                val conditions = DownloadConditions.Builder().build()
                remoteModelManager.download(model, conditions)
                    .addOnSuccessListener {
                        _mlKitDownloadState.value = null
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ModelDownloadManager", "Error downloading ML Kit model", e)
                        _mlKitDownloadState.value = false
                        onComplete(false)
                    }
            } else {
                _mlKitDownloadState.value = false
                onComplete(false)
            }
        } catch (e: Exception) {
            Log.e("ModelDownloadManager", "Error triggering ML Kit download", e)
            _mlKitDownloadState.value = false
            onComplete(false)
        }
    }
    
    fun resetStates() {
        _onnxDownloadProgress.value = null
        _mlKitDownloadState.value = null
    }
}
