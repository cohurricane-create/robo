package com.example.robo.ai

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.example.robo.model.Emotion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class LocalFaceAnalyzer(
    private val ownerManager: OwnerManager,
    private val onOwnerRecognition: (isOwner: Boolean, faceCount: Int, lookOffset: Offset, detectedEmotion: Emotion?) -> Unit,
    private val onFaceForRegistration: (Face) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    var isRegistrationMode = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val primaryFace = faces[0]

                        if (isRegistrationMode) {
                            onFaceForRegistration(primaryFace)
                        } else {
                            // Ko'pchilik ichida egasini qidirish
                            val foundOwner = faces.any { ownerManager.isOwner(it) }

                            // Yuzdagi jilmayish
                            val smileProb = primaryFace.smilingProbability ?: 0f
                            val detectedEmotion = if (smileProb > 0.65f) Emotion.HAPPY else null

                            // Nigohni yuz tomonga burish (Gaze Tracking)
                            val lookX = (-primaryFace.headEulerAngleY / 25f).coerceIn(-1f, 1f)
                            val lookY = (primaryFace.headEulerAngleX / 20f).coerceIn(-1f, 1f)

                            onOwnerRecognition(foundOwner, faces.size, Offset(lookX, lookY), detectedEmotion)
                        }
                    } else {
                        if (!isRegistrationMode) {
                            onOwnerRecognition(false, 0, Offset.Zero, null)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
