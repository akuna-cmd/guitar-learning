package com.guitarlearning.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guitarlearning.data.local.entity.TextNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextNoteDao {

    @Query("SELECT * FROM text_notes WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    fun getTextNotesForLesson(lessonId: String): Flow<List<TextNoteEntity>>

    @Query("SELECT * FROM text_notes ORDER BY createdAt DESC")
    suspend fun getAllTextNotes(): List<TextNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextNote(textNote: TextNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTextNotes(textNotes: List<TextNoteEntity>)

    @Update
    suspend fun updateTextNote(textNote: TextNoteEntity)

    @Delete
    suspend fun deleteTextNote(textNote: TextNoteEntity)

    @Query("DELETE FROM text_notes")
    suspend fun clearAll()
}
