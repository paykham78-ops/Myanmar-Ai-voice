package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_history")
data class VoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val voiceName: String,
    val speed: Float,
    val audioPath: String,
    val durationMs: Long = 0,
    val engineType: String = "Gemini AI",
    val createdAt: Long = System.currentTimeMillis()
)
