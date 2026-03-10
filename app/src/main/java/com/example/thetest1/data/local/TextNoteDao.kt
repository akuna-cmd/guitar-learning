package com.example.thetest1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thetest1.domain.model.TextNote
import kotlinx.coroutines.flow.Flow

@Dao
interface TextNoteDao {

    @Query("SELECT * FROM text_notes WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    fun getTextNotesForLesson(lessonId: String): Flow<List<TextNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextNote(textNote: TextNote)

    @Update
    suspend fun updateTextNote(textNote: TextNote)

    @Delete
    suspend fun deleteTextNote(textNote: TextNote)
}
