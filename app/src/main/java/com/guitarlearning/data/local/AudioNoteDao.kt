package com.guitarlearning.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guitarlearning.domain.model.AudioNote
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioNoteDao {

    @Query("SELECT * FROM audio_notes WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    fun getNotesForLesson(lessonId: String): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes ORDER BY createdAt DESC")
    suspend fun getAllNotes(): List<AudioNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioNote: AudioNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audioNotes: List<AudioNote>)

    @Update
    suspend fun update(audioNote: AudioNote)

    @Query("DELETE FROM audio_notes WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM audio_notes")
    suspend fun clearAll()
}
