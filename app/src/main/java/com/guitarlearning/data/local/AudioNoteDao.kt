package com.guitarlearning.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guitarlearning.domain.model.AudioNote
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioNoteDao {

    @Query("SELECT * FROM audio_notes WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    fun getNotesForLesson(lessonId: String): Flow<List<AudioNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioNote: AudioNote)

    @Query("DELETE FROM audio_notes WHERE id = :id")
    suspend fun delete(id: Int)
}
