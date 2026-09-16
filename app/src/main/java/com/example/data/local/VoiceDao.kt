package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voice_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<VoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voice: VoiceEntity): Long

    @Delete
    suspend fun delete(voice: VoiceEntity)

    @Query("DELETE FROM voice_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voice_history")
    suspend fun clearAll()
}
