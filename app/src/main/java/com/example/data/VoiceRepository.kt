package com.example.data

import android.content.Context
import com.example.backend.GenerateSpeechRequest
import com.example.backend.SpeechBackendService
import com.example.data.local.VoiceDao
import com.example.data.local.VoiceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VoiceRepository(
    private val context: Context,
    private val voiceDao: VoiceDao,
    private val backendService: SpeechBackendService = SpeechBackendService(context)
) {
    val allHistory: Flow<List<VoiceEntity>> = voiceDao.getAllHistory()

    suspend fun generateVoice(
        text: String,
        voiceName: String,
        speed: Float
    ): Result<VoiceEntity> = withContext(Dispatchers.IO) {
        val request = GenerateSpeechRequest(
            text = text,
            voice = voiceName
        )

        val backendResult = backendService.handleGenerateSpeech(request)

        backendResult.fold(
            onSuccess = { response ->
                val entity = VoiceEntity(
                    text = text.trim(),
                    voiceName = voiceName,
                    speed = speed,
                    audioPath = response.audioUrl,
                    durationMs = (response.duration * 1000).toLong(),
                    engineType = "Gemini TTS ($voiceName)"
                )
                val id = voiceDao.insert(entity)
                val savedEntity = entity.copy(id = id)
                Result.success(savedEntity)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun delete(voice: VoiceEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(voice.audioPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        voiceDao.delete(voice)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        voiceDao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        voiceDao.clearAll()
    }
}
