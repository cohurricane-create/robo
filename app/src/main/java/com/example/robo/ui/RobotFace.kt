package com.example.robo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.example.robo.model.Emotion
import com.example.robo.model.RobotState
import com.example.robo.ui.theme.EmoCyan
import com.example.robo.ui.theme.EmoDeepTeal
import com.example.robo.ui.theme.EmoGlow
import com.example.robo.ui.theme.EmoHeartPink
import com.example.robo.ui.theme.EmoTearBlue
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RobotFace(
    state: RobotState,
    modifier: Modifier = Modifier
) {
    // EMO silliq pirpiratishi
    var targetBlink by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(state.emotion) {
        if (state.emotion == Emotion.SLEEPY) {
            targetBlink = 0.06f
            return@LaunchedEffect
        }
        targetBlink = 1f
        while (true) {
            delay(Random.nextLong(2600, 5000))
            if (state.emotion != Emotion.SLEEPY) {
                targetBlink = 0.04f
                delay(90)
                targetBlink = 1f
            }
        }
    }

    val animatedBlinkScaleY by animateFloatAsState(
        targetValue = targetBlink,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "emo_blink"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "emo_life")

    // EMO jonli nafas olishi (Idle breathing)
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // EMO yurakcha urishi
    val heartPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    // EMO boshi aylangandagi spiral
    val dizzyAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dizzy"
    )

    // EMO qitiqlanish titrashi
    val tickleWiggle by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tickle"
    )

    // EMO elastik nigohi (Spring physics)
    val animatedLookX by animateFloatAsState(
        targetValue = state.lookTarget.x,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
        label = "lookX"
    )
    val animatedLookY by animateFloatAsState(
        targetValue = state.lookTarget.y,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
        label = "lookY"
    )

    // Ko'z o'lchamlari
    val eyeWidthMultiplier by animateFloatAsState(
        targetValue = when (state.emotion) {
            Emotion.SURPRISED -> 1.3f
            Emotion.SLEEPY -> 1.05f
            Emotion.HAPPY, Emotion.TICKLED -> 1.15f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f),
        label = "eyeWidth"
    )

    val eyeHeightMultiplier by animateFloatAsState(
        targetValue = when (state.emotion) {
            Emotion.SURPRISED -> 1.35f
            Emotion.SLEEPY -> 0.08f
            Emotion.HAPPY, Emotion.TICKLED -> 0.68f
            Emotion.SAD -> 0.82f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f),
        label = "eyeHeight"
    )

    val eyebrowAngle by animateFloatAsState(
        targetValue = when (state.emotion) {
            Emotion.ANGRY -> 24f
            Emotion.SAD -> -18f
            Emotion.SURPRISED -> -8f
            else -> 0f
        },
        animationSpec = tween(260),
        label = "eyebrowAngle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // EMO ning o'ziga xos katta, baquvvat ko'zlari
        val eyeBaseW = size.width * 0.23f
        val eyeBaseH = size.height * 0.29f
        val eyeDistance = size.width * 0.24f

        val currentEyeW = eyeBaseW * eyeWidthMultiplier
        val currentEyeH = eyeBaseH * eyeHeightMultiplier * animatedBlinkScaleY
        val isLove = state.emotion == Emotion.LOVE

        val facePrimary = if (isLove) EmoHeartPink else EmoCyan
        val faceSecondary = if (isLove) Color(0xFFFF529A) else EmoDeepTeal
        val faceGlow = if (isLove) EmoHeartPink.copy(alpha = 0.45f) else EmoGlow

        val wiggleOffset = if (state.emotion == Emotion.TICKLED) tickleWiggle else 0f

        scale(breathScale, breathScale, pivot = Offset(centerX, centerY)) {
            val lookClampX = (animatedLookX * size.width * 0.12f) + wiggleOffset
            val lookClampY = (animatedLookY * size.height * 0.08f)

            translate(left = lookClampX, top = lookClampY) {
                val leftEyeCenter = Offset(centerX - eyeDistance, centerY - size.height * 0.02f)
                val rightEyeCenter = Offset(centerX + eyeDistance, centerY - size.height * 0.02f)

                // EMO Ko'zlari
                when (state.emotion) {
                    Emotion.LOVE -> {
                        val loveSize = eyeBaseW * 1.25f * heartPulse
                        drawEmoHeart(leftEyeCenter, loveSize, facePrimary, faceGlow)
                        drawEmoHeart(rightEyeCenter, loveSize, facePrimary, faceGlow)
                    }
                    Emotion.HAPPY, Emotion.TICKLED -> {
                        // EMO ning mashhur quvonchli ko'zlari (^ ^)
                        drawEmoHappyEye(leftEyeCenter, currentEyeW, eyeBaseH * 0.75f, facePrimary, faceGlow)
                        drawEmoHappyEye(rightEyeCenter, currentEyeW, eyeBaseH * 0.75f, facePrimary, faceGlow)
                    }
                    Emotion.DIZZY -> {
                        drawEmoSpiral(leftEyeCenter, currentEyeW * 0.48f, dizzyAngle, facePrimary)
                        drawEmoSpiral(rightEyeCenter, currentEyeW * 0.48f, -dizzyAngle, facePrimary)
                    }
                    else -> {
                        // EMO ning original Squircle ko'zlari (Yumaloq to'rtburchak + nur akslari)
                        drawEmoSquircleEye(leftEyeCenter, currentEyeW, currentEyeH, facePrimary, faceSecondary, faceGlow, state.emotion)
                        drawEmoSquircleEye(rightEyeCenter, currentEyeW, currentEyeH, facePrimary, faceSecondary, faceGlow, state.emotion)
                    }
                }

                // Qoshlar (ANGRY, SAD, SURPRISED)
                if (state.emotion == Emotion.ANGRY || state.emotion == Emotion.SAD || state.emotion == Emotion.SURPRISED) {
                    val browLength = currentEyeW * 0.95f
                    val browOffset = currentEyeH / 2f + 22f

                    rotate(eyebrowAngle, pivot = leftEyeCenter) {
                        drawLine(
                            color = facePrimary,
                            start = Offset(leftEyeCenter.x - browLength / 2, leftEyeCenter.y - browOffset),
                            end = Offset(leftEyeCenter.x + browLength / 2, leftEyeCenter.y - browOffset),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                    }

                    rotate(-eyebrowAngle, pivot = rightEyeCenter) {
                        drawLine(
                            color = facePrimary,
                            start = Offset(rightEyeCenter.x - browLength / 2, rightEyeCenter.y - browOffset),
                            end = Offset(rightEyeCenter.x + browLength / 2, rightEyeCenter.y - browOffset),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Ko'z yoshi (SAD)
                if (state.emotion == Emotion.SAD) {
                    val tearCenter = Offset(rightEyeCenter.x + currentEyeW * 0.36f, rightEyeCenter.y + currentEyeH * 0.6f)
                    drawEmoTear(tearCenter, 14f, EmoTearBlue)
                }

                // EMO kichik yoqimli og'zi
                val mouthCenter = Offset(centerX, centerY + size.height * 0.22f)
                drawEmoMouth(mouthCenter, state.emotion, facePrimary, size.width * 0.16f)
            }
        }
    }
}

// EMO Squircle Ko'zi (Katta yumaloq to'rtburchak, yumshoq burchaklar va EMO highlight)
private fun DrawScope.drawEmoSquircleEye(
    center: Offset,
    w: Float,
    h: Float,
    primaryColor: Color,
    secondaryColor: Color,
    glowColor: Color,
    emotion: Emotion
) {
    val rect = Rect(center.x - w / 2f, center.y - h / 2f, center.x + w / 2f, center.y + h / 2f)
    // EMO squircle burchak radiusi
    val cornerRadius = CornerRadius(w * 0.38f, h * 0.38f)
    val roundRect = RoundRect(rect, cornerRadius)
    val path = Path().apply { addRoundRect(roundRect) }

    // Tashqi neon nur
    drawRoundRect(
        color = glowColor,
        topLeft = Offset(rect.left - 6f, rect.top - 6f),
        size = Size(rect.width + 12f, rect.height + 12f),
        cornerRadius = CornerRadius(w * 0.42f, h * 0.42f)
    )

    // EMO ko'k-firuza gradienti
    val eyeBrush = Brush.verticalGradient(
        colors = listOf(primaryColor, secondaryColor),
        startY = rect.top,
        endY = rect.bottom
    )
    drawPath(path, brush = eyeBrush)

    // EMO nur aksi (Highlight)
    if (h > 18f && emotion != Emotion.SLEEPY) {
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = w * 0.14f,
            center = Offset(center.x + w * 0.18f, center.y - h * 0.18f)
        )
    }
}

// EMO Quvonchli Ko'zi (^ ^)
private fun DrawScope.drawEmoHappyEye(center: Offset, w: Float, h: Float, color: Color, glowColor: Color) {
    val path = Path().apply {
        moveTo(center.x - w / 2f, center.y + h / 4f)
        cubicTo(
            center.x - w / 4f, center.y - h * 0.85f,
            center.x + w / 4f, center.y - h * 0.85f,
            center.x + w / 2f, center.y + h / 4f
        )
    }
    drawPath(path = path, color = glowColor, style = Stroke(width = 24f, cap = StrokeCap.Round))
    drawPath(path = path, color = color, style = Stroke(width = 18f, cap = StrokeCap.Round))
}

// EMO Yurakcha Ko'zi
private fun DrawScope.drawEmoHeart(center: Offset, size: Float, color: Color, glowColor: Color) {
    fun createHeartPath(scaleFactor: Float): Path = Path().apply {
        val w = size * scaleFactor
        val h = size * scaleFactor
        moveTo(center.x, center.y + h * 0.35f)
        cubicTo(
            center.x - w * 0.6f, center.y - h * 0.15f,
            center.x - w * 0.5f, center.y - h * 0.6f,
            center.x, center.y - h * 0.25f
        )
        cubicTo(
            center.x + w * 0.5f, center.y - h * 0.6f,
            center.x + w * 0.6f, center.y - h * 0.15f,
            center.x, center.y + h * 0.35f
        )
        close()
    }
    drawPath(path = createHeartPath(1.15f), color = glowColor)
    drawPath(path = createHeartPath(1.0f), color = color)
}

private fun DrawScope.drawEmoSpiral(center: Offset, radius: Float, angleDegrees: Float, color: Color) {
    rotate(angleDegrees, pivot = center) {
        val path = Path()
        val turns = 2.5f
        val points = 60
        for (i in 0..points) {
            val t = i.toFloat() / points
            val currentRadius = radius * t
            val angle = t * turns * 2 * Math.PI
            val x = center.x + (currentRadius * cos(angle)).toFloat()
            val y = center.y + (currentRadius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 9f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawEmoTear(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.5f)
        cubicTo(center.x - radius, center.y, center.x - radius, center.y + radius, center.x, center.y + radius)
        cubicTo(center.x + radius, center.y + radius, center.x + radius, center.y, center.x, center.y - radius * 1.5f)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawEmoMouth(
    center: Offset,
    emotion: Emotion,
    color: Color,
    width: Float
) {
    if (emotion == Emotion.SURPRISED || emotion == Emotion.DIZZY) {
        drawCircle(color = color, radius = width * 0.2f, center = center, style = Stroke(width = 8f))
    } else {
        val curve = when (emotion) {
            Emotion.HAPPY, Emotion.TICKLED -> 1f
            Emotion.IDLE -> 0.35f
            Emotion.LOVE -> 0.6f
            Emotion.SAD -> -0.8f
            Emotion.ANGRY -> -0.4f
            Emotion.SLEEPY -> 0.1f
            else -> 0f
        }
        val halfW = width / 2.2f
        val verticalOffset = curve * 20f
        val path = Path().apply {
            moveTo(center.x - halfW, center.y - verticalOffset * 0.3f)
            quadraticTo(center.x, center.y + verticalOffset, center.x + halfW, center.y - verticalOffset * 0.3f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 8f, cap = StrokeCap.Round))
    }
}
