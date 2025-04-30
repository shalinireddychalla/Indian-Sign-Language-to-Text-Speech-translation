package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var textViewResult: TextView
    private lateinit var handLandmarker: HandLandmarker
    private lateinit var spaceButton: Button
    private lateinit var clearButton: Button
    private lateinit var speakButton: Button  // New button for speaking text

    // Text-to-speech manager
    private lateinit var ttsManager: TextToSpeechManager

    private var lastSentTime = 0L
    private val resultBuilder = StringBuilder()

    companion object {
        const val CAMERA_PERMISSION_CODE = 1001
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        textViewResult = findViewById(R.id.resultText)
        spaceButton = findViewById(R.id.spaceButton)
        clearButton = findViewById(R.id.clearButton)
        speakButton = findViewById(R.id.speakButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        ttsManager = TextToSpeechManager(this)

        spaceButton.setOnClickListener {
            resultBuilder.append(" ")
            textViewResult.text = resultBuilder.toString()
        }

        clearButton.setOnClickListener {
            resultBuilder.clear()
            textViewResult.text = ""
            ttsManager.stop()
        }

        speakButton.setOnClickListener {
            val textToSpeak = resultBuilder.toString()
            if (textToSpeak.isNotEmpty()) {
                ttsManager.speak(textToSpeak)
            } else {
                Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            initLandmarker()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun initLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(2)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(this, options)
            Log.d(TAG, "HandLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing HandLandmarker: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize hand tracking: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image -> analyzeImage(image) }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSentTime > 1500) {
            val bitmap = image.toBitmap()
            if (bitmap == null) {
                Log.e(TAG, "Bitmap conversion failed — skipping frame")
                image.close()
                return
            }

            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = handLandmarker.detect(mpImage)

            if (result.landmarks().isEmpty()) {
                Log.d(TAG, "No hand detected — skipping frame")
                image.close()
                return
            }

            val landmarksArray = extractLandmarks(result)
            Log.d(TAG, "Extracted landmarks preview: ${landmarksArray.take(10).joinToString(", ")}")

            sendLandmarksToBackend(landmarksArray)
            lastSentTime = currentTime
        }
        image.close()
    }

    private fun extractLandmarks(result: HandLandmarkerResult): FloatArray {
        val totalLandmarks = 2 * 21 * 3
        val landmarkArray = FloatArray(totalLandmarks) { 0f }

        result.landmarks().take(2).forEachIndexed { handIndex, handLandmarks ->
            val offset = handIndex * 63
            handLandmarks.forEachIndexed { i, landmark ->
                landmarkArray[offset + i * 3] = landmark.x()
                landmarkArray[offset + i * 3 + 1] = landmark.y()
                landmarkArray[offset + i * 3 + 2] = landmark.z()
            }
        }

        return landmarkArray
    }

    private fun sendLandmarksToBackend(landmarks: FloatArray) {
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("landmarks", JSONArray(landmarks.toList()))
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("YOUR_CLOUDFLARE_URL/predict")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e(TAG, "Network error", e)
                runOnUiThread {
                    textViewResult.text = "Network Error: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d(TAG, "Backend raw response: $responseData")

                runOnUiThread {
                    try {
                        val jsonObject = JSONObject(responseData ?: "{}")
                        val prediction = jsonObject.optString("prediction", "No Hand")

                        if (prediction != "No Hand" && prediction != "_") {
                            Log.d(TAG, "Predicted: $prediction")
                            resultBuilder.append(prediction)
                            textViewResult.text = resultBuilder.toString()
                        } else {
                            Log.d(TAG, "Prediction skipped: $prediction")
                        }
                    } catch (e: Exception) {
                        textViewResult.text = "Parsing Error: ${e.message}"
                        Log.e(TAG, "JSON Parsing Error", e)
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLandmarker()
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        cameraExecutor.shutdown()
    }
}

fun ImageProxy.toBitmap(): android.graphics.Bitmap? {
    return try {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()

        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        Log.e("toBitmap", "Conversion failed: ${e.message}")
        null
    }
}