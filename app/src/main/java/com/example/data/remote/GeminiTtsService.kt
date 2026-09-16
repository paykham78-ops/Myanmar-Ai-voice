package com.example.data.remote

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiTtsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateSpeech(
        apiKey: String,
        text: String,
        voiceName: String = "Puck"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("API key is not configured"))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"

            val promptText = "Read the following Burmese text aloud with natural pronunciation, pitch, and cadence:\n\n$text"

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", promptText)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    val modalities = JSONArray().apply {
                        put("AUDIO")
                    }
                    put("responseModalities", modalities)

                    val speechConfig = JSONObject().apply {
                        val voiceConfig = JSONObject().apply {
                            val prebuiltVoiceConfig = JSONObject().apply {
                                put("voiceName", voiceName)
                            }
                            put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                        }
                        put("voiceConfig", voiceConfig)
                    }
                    put("speechConfig", speechConfig)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(IOException("No candidates returned from Gemini TTS"))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val base64Data = inlineData.optString("data")
                        if (base64Data.isNotBlank()) {
                            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            return@withContext Result.success(audioBytes)
                        }
                    }
                }
            }

            Result.failure(IOException("Audio data not found in response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
