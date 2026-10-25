package com.example.robo.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun askRobo(userPrompt: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", """
                            Sening isming Robo. Sen telefon ichida yashovchi yoqimli, samimiy va chaqqon kichik robot-uy hayvonisan (EMO kabi).
                            Foydalanuvchi bilan o'zbek tilida gaplash.
                            Javoblaring juda qisqa (1-2 ta ixcham gap) va his-tuyg'uli bo'lsin.
                            Har bir javobingning eng boshida bitta emotsiyani qavsda yoz:
                            [HAPPY], [SAD], [SURPRISED], [ANGRY], [LOVE], yoki [IDLE].
                            Misol: "[HAPPY] Salom do'stim! Bugun kayfiyating qanday?"
                        """.trimIndent())
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
                put("max_tokens", 90)
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val jsonResponse = JSONObject(responseBody)
            val fullText = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val emotionTag = Regex("\\[(.*?)\\]").find(fullText)?.groupValues?.get(1)?.uppercase() ?: "IDLE"
            val cleanSpeech = fullText.replace(Regex("\\[.*?\\]"), "").trim()

            Pair(emotionTag, cleanSpeech)
        } catch (e: Exception) {
            Pair("SAD", "Internet bilan muammo bo'ldi...")
        }
    }
}
