package com.example.robo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robo.ui.theme.EmoCyan
import com.example.robo.ui.theme.LooiTrueBlack
import com.example.robo.viewmodel.RobotViewModel
import kotlin.math.abs

@Composable
fun RobotScreen(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LooiTrueBlack)
            // LOOI Desktop Robot - Butun ekran sensorli
            .pointerInput(Unit) {
                if (state.isSetupMode) return@pointerInput
                detectTapGestures(
                    onTap = { viewModel.onPoke() },
                    onDoubleTap = { viewModel.onPetting() },
                    onLongPress = { viewModel.onPetting() }
                )
            }
            .pointerInput(Unit) {
                if (state.isSetupMode) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        totalDragDistance = 0f
                        dragStartTime = System.currentTimeMillis()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dist = abs(dragAmount.x) + abs(dragAmount.y)
                        totalDragDistance += dist
                    },
                    onDragEnd = {
                        val duration = System.currentTimeMillis() - dragStartTime
                        if (totalDragDistance > 400f && duration < 1200) {
                            viewModel.onTickle()
                        } else if (totalDragDistance > 70f) {
                            viewModel.onPetting()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // EMO Ko'zli LOOI Robot Yuzi
        RobotFace(
            state = state,
            modifier = Modifier.fillMaxSize()
        )

        // Egasi bilan birinchi tanishuv
        if (state.isSetupMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp, start = 36.dp, end = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤖 LOOI tanishuv rejimi",
                    color = EmoCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.setupScanProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmoCyan,
                    trackColor = Color(0xFF14202C)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Yuzingizni kameraga qaratib turing...",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Shaffof ixcham suhbat matni
        AnimatedVisibility(
            visible = state.speechText.isNotBlank() || state.isListening || state.isThinking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 54.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xD90A111C))
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = when {
                        state.isListening -> "🎙 Tinglayapman..."
                        state.isThinking -> "💭 O'ylayapman..."
                        else -> state.speechText
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
