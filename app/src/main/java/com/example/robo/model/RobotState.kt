package com.example.robo.model

import androidx.compose.ui.geometry.Offset

data class RobotState(
    val emotion: Emotion = Emotion.IDLE,
    val lookTarget: Offset = Offset.Zero,
    val speechText: String = "",
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val affectionLevel: Int = 50,
    val isSetupMode: Boolean = false,         // Egasining yuzini skanerlab tanishish rejimi
    val setupScanProgress: Float = 0f,        // Skanerlash jarayoni 0f..1f
    val isOwnerRecognized: Boolean = false    // Aynan egasini ko'rib turganlik holati
)
