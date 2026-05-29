package com.guitarlearning.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guitarlearning.data.local.entity.AudioNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioNoteDao {

    @Query("SELECT * FROM audio_notes WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    fun getNotesForLesson(lessonId: String): Flow<List<AudioNoteEntity>>

    @Query("SELECT * FROM audio_notes ORDER BY createdAt DESC")
    suspend fun getAllNotes(): List<AudioNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioNote: AudioNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audioNotes: List<AudioNoteEntity>)

    @Update
    suspend fun update(audioNote: AudioNoteEntity)

    @Query("DELETE FROM audio_notes WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM audio_notes")
    suspend fun clearAll()
}
