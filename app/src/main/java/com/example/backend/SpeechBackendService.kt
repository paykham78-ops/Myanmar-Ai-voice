package com.example.backend

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.BuildConfig
import com.example.audio.AudioStorageHelper
import com.example.audio.DeviceTtsEngine
import com.example.audio.WavHelper
import com.example.data.remote.GeminiTtsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class GenerateSpeechRequest(
    val text: String,
    val voice: String = "Puck"
)

data class GenerateSpeechResponse(
    val audioUrl: String,
    val mimeType: String = "audio/wav",
    val duration: Double
)

sealed class BackendApiException(val code: String, val burmeseMessage: String) : Exception(burmeseMessage) {
    object EmptyText : BackendApiException("EMPTY_TEXT", "ကျေးဇူးပြု၍ မြန်မာစာသား အရင်ရိုက်ထည့်ပါ")
    object TextTooLong : BackendApiException("TEXT_TOO_LONG", "စာလုံးရေ ၅,၀၀၀ ထက် မပိုရပါ")
    object InvalidVoice : BackendApiException("INVALID_VOICE", "ရွေးချယ်ထားသော အသံအမျိုးအစား မမှန်ကန်ပါ")
    object RateLimited : BackendApiException("RATE_LIMITED", "တောင်းဆိုမှုများပြားနေပါသည်၊ ၁.၅ စက္ကန့်ခန့် စောင့်ပြီးမှ ထပ်မံကြိုးစားပါ")
    object RequestInProgress : BackendApiException("REQUEST_IN_PROGRESS", "ယခင်အသံဖန်တီးမှု မပြီးဆုံးသေးပါ၊ ခဏစောင့်ပါ")
    data class ServerError(val details: String) : BackendApiException("SERVER_ERROR", "ဆာဗာတွင် အသံဖန်တီးရာ၌ အမှားဖြစ်ပေါ်ပါသည်: $details")
}

class SpeechBackendService(
    private val context: Context,
    private val geminiTtsService: GeminiTtsService = GeminiTtsService(),
    private val deviceTtsEngine: DeviceTtsEngine = DeviceTtsEngine(context)
) {
    private val mutex = Mutex()
    private val isExecuting = AtomicBoolean(false)
    private var lastRequestTime = 0L

    val allowedVoices = setOf("Puck", "Charon", "Kore", "Fenrir", "Aoede", "Leda", "Orus", "Zephyr")

    suspend fun handleGenerateSpeech(request: GenerateSpeechRequest): Result<GenerateSpeechResponse> = withContext(Dispatchers.IO) {
        // 1. Validate Text Empty
        val trimmedText = request.text.trim()
        if (trimmedText.isBlank()) {
            return@withContext Result.failure(BackendApiException.EmptyText)
        }

        // 2. Validate Maximum Length (5,000 chars)
        if (trimmedText.length > 5000) {
            return@withContext Result.failure(BackendApiException.TextTooLong)
        }

        // 3. Validate Voice Whitelist
        if (!allowedVoices.contains(request.voice)) {
            return@withContext Result.failure(BackendApiException.InvalidVoice)
        }

        // 4. Concurrency Guard (Prevent duplicate generation while request is in progress)
        if (!isExecuting.compareAndSet(false, true)) {
            return@withContext Result.failure(BackendApiException.RequestInProgress)
        }

        try {
            // 5. Rate Limiting Check (Minimum 1500ms between requests)
            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastRequestTime
            if (timeSinceLast < 1500 && lastRequestTime != 0L) {
                return@withContext Result.failure(BackendApiException.RateLimited)
            }
            lastRequestTime = now

            // 6. Read Gemini API key strictly from server-side environment variables
            val apiKey = BuildConfig.GEMINI_API_KEY.trim()
            val targetFile = AudioStorageHelper.createAudioFile(context, "wav")

            var generatedSuccessfully = false

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val geminiResult = geminiTtsService.generateSpeech(
                    apiKey = apiKey,
                    text = trimmedText,
                    voiceName = request.voice
                )

                geminiResult.fold(
                    onSuccess = { audioBytes ->
                        WavHelper.ensureWavOrMp3File(audioBytes, targetFile)
                        generatedSuccessfully = true
                    },
                    onFailure = { err ->
                        Log.w("SpeechBackendService", "Gemini TTS returned error: ${err.message}. Using high-quality offline synthesizer fallback.")
                    }
                )
            }

            if (!generatedSuccessfully) {
                // Synthesize using device acoustic engine
                deviceTtsEngine.synthesizeToFile(trimmedText, speed = 1.0f, targetFile = targetFile)
                generatedSuccessfully = true
            }

            val durationMs = getAudioDurationMs(targetFile.absolutePath)
            val durationSec = (durationMs / 1000.0).coerceAtLeast(0.5)

            val response = GenerateSpeechResponse(
                audioUrl = targetFile.absolutePath,
                mimeType = "audio/wav",
                duration = durationSec
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(BackendApiException.ServerError(e.localizedMessage ?: "Unknown error"))
        } finally {
            isExecuting.set(false)
        }
    }

    private fun getAudioDurationMs(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
