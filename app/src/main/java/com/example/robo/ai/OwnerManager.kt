package com.example.robo.ai

import android.content.Context
import android.content.SharedPreferences
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.sqrt

class OwnerManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("robo_owner_prefs", Context.MODE_PRIVATE)

    var isOwnerRegistered: Boolean
        get() = prefs.getBoolean("is_registered", false)
        set(value) = prefs.edit().putBoolean("is_registered", value).apply()

    // Egasining yuz nisbatlarini o'lchash va saqlash
    fun registerOwnerFace(face: Face): Boolean {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return false
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return false
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position ?: return false
        val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: return false

        val eyeDistance = distance(leftEye.x, leftEye.y, rightEye.x, rightEye.y)
        if (eyeDistance <= 0f) return false

        // Yuz nisbatlari (Masofaga bog'liq bo'lmagan normalizatsiyalangan ko'rsatkichlar)
        val eyeNoseRatio = distance((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f, nose.x, nose.y) / eyeDistance
        val noseMouthRatio = distance(nose.x, nose.y, mouth.x, mouth.y) / eyeDistance

        prefs.edit()
            .putFloat("eye_nose_ratio", eyeNoseRatio)
            .putFloat("nose_mouth_ratio", noseMouthRatio)
            .putBoolean("is_registered", true)
            .apply()

        return true
    }

    // Ko'ringan yuz egasiga tegishlimi yoki begona?
    fun isOwner(face: Face): Boolean {
        if (!isOwnerRegistered) return false

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return false
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return false
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position ?: return false
        val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: return false

        val eyeDistance = distance(leftEye.x, leftEye.y, rightEye.x, rightEye.y)
        if (eyeDistance <= 0f) return false

        val currentEyeNoseRatio = distance((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f, nose.x, nose.y) / eyeDistance
        val currentNoseMouthRatio = distance(nose.x, nose.y, mouth.x, mouth.y) / eyeDistance

        val savedEyeNose = prefs.getFloat("eye_nose_ratio", 0f)
        val savedNoseMouth = prefs.getFloat("nose_mouth_ratio", 0f)

        val diff1 = abs(currentEyeNoseRatio - savedEyeNose)
        val diff2 = abs(currentNoseMouthRatio - savedNoseMouth)

        // 80%+ o'xshashlik bo'lsa egasi deb tan oladi
        return diff1 < 0.15f && diff2 < 0.15f
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
