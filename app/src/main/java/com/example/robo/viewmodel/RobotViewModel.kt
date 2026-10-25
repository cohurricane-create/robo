package com.example.robo.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.robo.ai.OpenAiService
import com.example.robo.ai.OwnerManager
import com.example.robo.model.Emotion
import com.example.robo.model.RobotState
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class RobotViewModel : ViewModel() {

    private val openAiService = OpenAiService(
        apiKey = BuildConfig.OPENAI_API_KEY
    )

    private val _uiState = MutableStateFlow(RobotState())
    val uiState: StateFlow<RobotState> = _uiState.asStateFlow()

    private var temporaryEmotionJob: Job? = null
    private var autonomousLifeJob: Job? = null
    private var idleTimerJob: Job? = null

    private var ownerManager: OwnerManager? = null
    private var wasFaceVisible = true
    private var lastOwnerGreetTime = 0L

    fun initOwnerManager(manager: OwnerManager) {
        this.ownerManager = manager
        if (!manager.isOwnerRegistered) {
            // Birinchi marta kirganda tanishuv ekrani
            _uiState.update {
                it.copy(
                    isSetupMode = true,
                    emotion = Emotion.SURPRISED,
                    speechText = "Salom! Men yangi robotman. Keling tanishamiz, yuzingizni kameraga qaratib turing!"
                )
            }
        } else {
            startAutonomousLife()
        }
    }

    // Tanishuv paytida yuzni skanerlab saqlash
    fun registerFace(face: Face, onComplete: () -> Unit) {
        val manager = ownerManager ?: return
        val currentProgress = _uiState.value.setupScanProgress + 0.35f
        if (currentProgress < 1f) {
            _uiState.update { it.copy(setupScanProgress = currentProgress) }
        } else {
            val success = manager.registerOwnerFace(face)
            if (success) {
                _uiState.update {
                    it.copy(
                        isSetupMode = false,
                        setupScanProgress = 1f,
                        emotion = Emotion.LOVE,
                        speechText = "Tanishganimdan judayam xursandman, mening egam!"
                    )
                }
                onComplete()
                viewModelScope.launch {
                    delay(3000L)
                    _uiState.update { it.copy(emotion = Emotion.IDLE, speechText = "") }
                    startAutonomousLife()
                }
            }
        }
    }

    // Ko'pchilik ichida egasini ko'rgandagi reaktsiya
    fun onOwnerRecognitionUpdate(isOwner: Boolean, faceCount: Int, lookOffset: Offset, detectedEmotion: Emotion?) {
        if (_uiState.value.isSetupMode || _uiState.value.isThinking) return

        if (faceCount > 0) {
            resetSleepTimer() // Kamerada inson bor ekan, robot aslo uxlamaydi!
        }

        onCameraVisibilityChanged(faceCount > 0)
        _uiState.update { it.copy(lookTarget = lookOffset) }

        if (isOwner) {
            // Egasini ko'rganda (har 15 soniyada 1 marta alohida xursand bo'ladi)
            val now = System.currentTimeMillis()
            if (now - lastOwnerGreetTime > 15_000L && _uiState.value.emotion != Emotion.LOVE) {
                lastOwnerGreetTime = now
                triggerTemporaryEmotion(Emotion.LOVE, 3500L, "Egam keldi! Salom do'stim!")
            } else if (detectedEmotion != null && _uiState.value.emotion == Emotion.IDLE) {
                triggerTemporaryEmotion(detectedEmotion, 2000L)
            }
        } else if (faceCount > 0 && _uiState.value.emotion == Emotion.IDLE) {
            // Begona odam qaraganda: shunchaki qiziqib, ehtiyotkor bo'lib turadi
            if (Random.nextInt(10) == 0) {
                triggerTemporaryEmotion(Emotion.SURPRISED, 1500L)
            }
        }
    }

    private fun startAutonomousLife() {
        autonomousLifeJob?.cancel()
        autonomousLifeJob = viewModelScope.launch {
            while (true) {
                delay(Random.nextLong(3500, 7500))
                if (_uiState.value.emotion == Emotion.IDLE && !_uiState.value.isThinking && !_uiState.value.isListening) {
                    val randomLookX = Random.nextFloat() * 1.5f - 0.75f
                    val randomLookY = Random.nextFloat() * 1.0f - 0.5f
                    _uiState.update { it.copy(lookTarget = Offset(randomLookX, randomLookY)) }
                    delay(Random.nextLong(1000, 2500))
                    _uiState.update { it.copy(lookTarget = Offset.Zero) }
                }
            }
        }
        resetSleepTimer()
    }

    private fun resetSleepTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = viewModelScope.launch {
            delay(180_000L) // 3 daqiqa davomida umuman hech qanday harakat bo'lmasagina uxlash
            setEmotion(Emotion.SLEEPY, "")
        }
    }

    private fun onInteraction() {
        resetSleepTimer()
        if (_uiState.value.emotion == Emotion.SLEEPY) {
            triggerTemporaryEmotion(Emotion.SURPRISED, 1600L)
        }
    }

    fun onPetting() {
        onInteraction()
        val currentAffection = _uiState.value.affectionLevel
        val nextAffection = (currentAffection + 5).coerceAtMost(100)
        _uiState.update { it.copy(affectionLevel = nextAffection) }
        triggerTemporaryEmotion(if (nextAffection > 75) Emotion.LOVE else Emotion.HAPPY, 2200L)
    }

    fun onTickle() {
        onInteraction()
        triggerTemporaryEmotion(Emotion.TICKLED, 2200L)
    }

    fun onPoke() {
        onInteraction()
        triggerTemporaryEmotion(Emotion.SURPRISED, 1500L)
    }

    fun onShake() {
        onInteraction()
        triggerTemporaryEmotion(Emotion.DIZZY, 3000L)
    }

    fun onCameraVisibilityChanged(isFaceVisible: Boolean) {
        if (!wasFaceVisible && isFaceVisible) {
            onInteraction()
            triggerTemporaryEmotion(Emotion.HAPPY, 2500L)
        }
        wasFaceVisible = isFaceVisible
    }

    fun onWakeWordDetected() {
        onInteraction()
        _uiState.update {
            it.copy(
                emotion = Emotion.SURPRISED,
                speechText = "Eshitaman...",
                isListening = true
            )
        }
    }

    fun sendQuestionToRobo(question: String, onResponseReady: (String) -> Unit) {
        onInteraction()
        _uiState.update {
            it.copy(
                isListening = false,
                isThinking = true,
                speechText = "..."
            )
        }

        viewModelScope.launch {
            val (emotionStr, replyText) = openAiService.askRobo(question)
            val emotion = try {
                Emotion.valueOf(emotionStr)
            } catch (_: Exception) {
                Emotion.HAPPY
            }

            _uiState.update {
                it.copy(
                    isThinking = false,
                    emotion = emotion,
                    speechText = replyText
                )
            }
            onResponseReady(replyText)
        }
    }

    fun setEmotion(emotion: Emotion, text: String = "") {
        temporaryEmotionJob?.cancel()
        resetSleepTimer()
        _uiState.update {
            it.copy(
                emotion = emotion,
                speechText = text
            )
        }
    }

    private fun triggerTemporaryEmotion(emotion: Emotion, durationMs: Long, phrase: String = "") {
        temporaryEmotionJob?.cancel()
        temporaryEmotionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    emotion = emotion,
                    speechText = phrase
                )
            }
            delay(durationMs)
            _uiState.update {
                it.copy(
                    emotion = Emotion.IDLE,
                    speechText = if (phrase.isNotBlank()) "" else it.speechText
                )
            }
        }
    }
}
