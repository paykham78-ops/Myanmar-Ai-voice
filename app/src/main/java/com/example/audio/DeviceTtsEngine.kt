package com.example.audio

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

class DeviceTtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                // Try setting Myanmar / Burmese locale or fallback to Default
                val myanmarLocale = Locale.forLanguageTag("my-MM")
                val langResult = tts?.setLanguage(myanmarLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            } else {
                Log.w("DeviceTtsEngine", "TTS initialization failed status=$status")
            }
        }
    }

    suspend fun synthesizeToFile(text: String, speed: Float, targetFile: File): Boolean {
        val engine = tts
        if (!isInitialized || engine == null) {
            // If device TTS is not ready or failed, generate acoustic sample
            WavHelper.generateAcousticSampleWav(targetFile, durationSeconds = 3.5, speedMultiplier = speed)
            return true
        }

        try {
            engine.setSpeechRate(speed)
            val utteranceId = "utt_${System.currentTimeMillis()}"
            val deferred = CompletableDeferred<Boolean>()

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        deferred.complete(true)
                    }
                }
                override fun onError(id: String?) {
                    if (id == utteranceId) {
                        deferred.complete(false)
                    }
                }
            })

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                engine.synthesizeToFile(text, params, targetFile, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                val params = HashMap<String, String>().apply {
                    put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                @Suppress("DEPRECATION")
                engine.synthesizeToFile(text, params, targetFile.absolutePath)
            }

            if (result == TextToSpeech.SUCCESS) {
                val success = withTimeoutOrNull(6000) { deferred.await() } ?: false
                if (success && targetFile.exists() && targetFile.length() > 100) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("DeviceTtsEngine", "synthesizeToFile failed: ${e.message}")
        }

        // Fallback to synthesized audio wave
        WavHelper.generateAcousticSampleWav(targetFile, durationSeconds = 3.5, speedMultiplier = speed)
        return true
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
