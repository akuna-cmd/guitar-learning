package com.example.thetest1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.thetest1.domain.model.AudioNote
import com.example.thetest1.domain.model.Goal
import com.example.thetest1.domain.model.Session
import com.example.thetest1.domain.model.TabItem
import com.example.thetest1.domain.model.TextNote

@Database(
    entities = [AudioNote::class, TextNote::class, Session::class, TabItem::class, Goal::class],
    version = 10
)
@TypeConverters(Converters::class, PracticedTabConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioNoteDao(): AudioNoteDao
    abstract fun textNoteDao(): TextNoteDao
    abstract fun sessionDao(): SessionDao
    abstract fun tabDao(): TabDao
    abstract fun goalDao(): GoalDao
}
