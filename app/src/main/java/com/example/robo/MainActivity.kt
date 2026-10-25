package com.example.robo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.robo.ai.LocalFaceAnalyzer
import com.example.robo.ai.OwnerManager
import com.example.robo.ui.RobotScreen
import com.example.robo.ui.theme.RoboTheme
import com.example.robo.viewmodel.RobotViewModel
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private val robotViewModel: RobotViewModel by viewModels()
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var ownerManager: OwnerManager
    private var faceAnalyzer: LocalFaceAnalyzer? = null

    // Akselerometr (Shake sensori)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startSpeechListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ownerManager = OwnerManager(this)
        robotViewModel.initOwnerManager(ownerManager)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("uz")
                // Robot gapirib bo'lgach, darhol keyingi savolni tinglashni boshlash
                textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            // Robot gapirib bo'ldi, endi yana uzluksiz tinglash rejimiga o'tadi
                            startSpeechListening()
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            startSpeechListening()
                        }
                    }
                })
            }
        }

        val hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera || !hasAudio) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        } else {
            startCamera()
            startSpeechListening()
        }

        setContent {
            RoboTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RobotScreen(viewModel = robotViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta

        if (acceleration > 12f) {
            robotViewModel.onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Kamera orqali egasini tanish va skanerlash
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            faceAnalyzer = LocalFaceAnalyzer(
                ownerManager = ownerManager,
                onOwnerRecognition = { isOwner, faceCount, lookOffset, detectedEmotion ->
                    runOnUiThread {
                        robotViewModel.onOwnerRecognitionUpdate(isOwner, faceCount, lookOffset, detectedEmotion)
                    }
                },
                onFaceForRegistration = { face ->
                    runOnUiThread {
                        robotViewModel.registerFace(face) {
                            // Tanishuv muvaffaqiyatli yakunlandi
                            faceAnalyzer?.isRegistrationMode = false
                        }
                    }
                }
            ).apply {
                isRegistrationMode = !ownerManager.isOwnerRegistered
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, faceAnalyzer!!)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startSpeechListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        runOnUiThread {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // Agar vaqt tugab qolsa (timeout) yoki sukunat bo'lsa, avtomatik qayta kutish
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                            runOnUiThread {
                                if (textToSpeech?.isSpeaking != true) {
                                    startSpeechListening()
                                }
                            }
                        } else {
                            speechRecognizer?.destroy()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val heardText = matches?.firstOrNull()?.trim() ?: ""

                        if (heardText.isNotBlank()) {
                            val lower = heardText.lowercase()
                            val question = lower.replace("robo", "").replace("hey", "").replace("salom", "").trim()
                            val finalQuestion = if (question.isBlank()) heardText else question

                            robotViewModel.sendQuestionToRobo(finalQuestion) { reply ->
                                textToSpeech?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "ROBO_TTS")
                            }
                        } else {
                            // Agar hech narsa aytilmagan bo'lsa, qayta tinglashni davom ettirish
                            runOnUiThread {
                                if (textToSpeech?.isSpeaking != true) {
                                    startSpeechListening()
                                }
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
