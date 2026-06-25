package io.github.serg987.sudokueinkhtr

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import com.onyx.android.sdk.data.note.TouchPoint
import kotlin.coroutines.resume


class MlKitDigitRecognizer(context: Context) {

    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer? = null
    private var isReady = false

    init {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            if (modelIdentifier != null) {
                model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model!!).build()
                )
                
                // The model download is now triggered on app startup via the companion object
                // We just assume it's ready or will be ready soon.
                isReady = true
            } else {
                Log.e("MlKitDigitRecognizer", "Model Identifier is null")
            }
        } catch (e: Exception) {
            Log.e("MlKitDigitRecognizer", "Init Error", e)
        }
    }

    class RecognitionResult(
        val digit: Int?,
        val candidates: List<String>,
        val processingTimeMs: Long
    )

    suspend fun recognizeDigitAsync(
        pathsList: List<List<DrawingPoint>>,
        cellWidth: Float,
        cellHeight: Float
    ): RecognitionResult? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        if (!isReady || recognizer == null) {
            Log.w("MlKitDigitRecognizer", "Recognizer not ready")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val start = System.currentTimeMillis()

        try {
            val inkBuilder = Ink.builder()

            for (path in pathsList) {
                val strokeBuilder = Ink.Stroke.builder()
                for (point in path) {
                    strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, point.timestamp))
                }
                inkBuilder.addStroke(strokeBuilder.build())
            }

            val ink = inkBuilder.build()

            val recognitionContext = RecognitionContext.builder()
                .setWritingArea(WritingArea(cellWidth, cellHeight))
                .setPreContext("0123456789")
                .build()

            recognizer!!.recognize(ink, recognitionContext)
                .addOnSuccessListener { result ->
                    val time = System.currentTimeMillis() - start
                    
                    val digitCandidate = result.candidates
                        .map { it.text.trim() }
                        .firstOrNull { it.matches(Regex("[0-9]")) }

                    val digit = digitCandidate?.toIntOrNull()
                    val candidates = result.candidates.map { it.text }
                    
                    if (cont.isActive) {
                        cont.resume(RecognitionResult(digit, candidates, time))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MlKitDigitRecognizer", "Recognition failure", e)
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
        } catch (e: Exception) {
            Log.e("MlKitDigitRecognizer", "Recognition Exception", e)
            if (cont.isActive) {
                cont.resume(null)
            }
        }
    }

    fun close() {
        recognizer?.close()
    }

    companion object {
        fun downloadModelIfNeeded() {
            try {
                val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
                if (modelIdentifier != null) {
                    val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                    val remoteModelManager = RemoteModelManager.getInstance()
                    remoteModelManager.download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener {
                            Log.i("MlKitDigitRecognizer", "Model download check completed successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("MlKitDigitRecognizer", "Error downloading model", e)
                        }
                }
            } catch (e: Exception) {
                Log.e("MlKitDigitRecognizer", "Download Init Error", e)
            }
        }
    }
}
