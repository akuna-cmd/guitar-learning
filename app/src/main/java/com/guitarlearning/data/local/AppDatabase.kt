package com.guitarlearning.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guitarlearning.domain.model.AudioNote
import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.TextNote
import java.util.UUID

@Database(
    entities = [
        AudioNote::class,
        TextNote::class,
        SessionEntity::class,
        PracticedTabEntity::class,
        TabItem::class,
        Goal::class
    ],
    version = 14
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioNoteDao(): AudioNoteDao
    abstract fun textNoteDao(): TextNoteDao
    abstract fun sessionDao(): SessionDao
    abstract fun tabDao(): TabDao
    abstract fun goalDao(): GoalDao
}

val Migration13To14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE audio_notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE text_notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val Migration12To13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tabs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goals_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                syncId TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                target INTEGER NOT NULL,
                progress INTEGER NOT NULL,
                deadline INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                isOverdue INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.query(
            """
            SELECT id, type, description, target, progress, deadline, isCompleted, isOverdue
            FROM goals
            """.trimIndent()
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val descriptionIndex = cursor.getColumnIndexOrThrow("description")
            val targetIndex = cursor.getColumnIndexOrThrow("target")
            val progressIndex = cursor.getColumnIndexOrThrow("progress")
            val deadlineIndex = cursor.getColumnIndexOrThrow("deadline")
            val isCompletedIndex = cursor.getColumnIndexOrThrow("isCompleted")
            val isOverdueIndex = cursor.getColumnIndexOrThrow("isOverdue")

            while (cursor.moveToNext()) {
                database.execSQL(
                    """
                    INSERT INTO goals_new (
                        id, syncId, type, description, target, progress, deadline, updatedAt, isCompleted, isOverdue
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        cursor.getInt(idIndex),
                        UUID.randomUUID().toString(),
                        cursor.getString(typeIndex),
                        cursor.getString(descriptionIndex),
                        cursor.getInt(targetIndex),
                        cursor.getInt(progressIndex),
                        cursor.getLong(deadlineIndex),
                        0L,
                        cursor.getInt(isCompletedIndex),
                        cursor.getInt(isOverdueIndex)
                    )
                )
            }
        }

        database.execSQL("DROP TABLE goals")
        database.execSQL("ALTER TABLE goals_new RENAME TO goals")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_syncId ON goals(syncId)")
    }
}
