package com.guitarlearning.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guitarlearning.data.local.dao.AudioNoteDao
import com.guitarlearning.data.local.dao.GoalDao
import com.guitarlearning.data.local.dao.SessionDao
import com.guitarlearning.data.local.dao.TabDao
import com.guitarlearning.data.local.dao.TextNoteDao
import com.guitarlearning.data.local.entity.AudioNoteEntity
import com.guitarlearning.data.local.entity.GoalEntity
import com.guitarlearning.data.local.entity.PracticedTabEntity
import com.guitarlearning.data.local.entity.SessionEntity
import com.guitarlearning.data.local.entity.TabTagCrossRef
import com.guitarlearning.data.local.entity.TabEntity
import com.guitarlearning.data.local.entity.TagEntity
import com.guitarlearning.data.local.entity.TextNoteEntity
import com.guitarlearning.data.tabs.normalizeTags
import java.util.UUID

@Database(
    entities = [
        AudioNoteEntity::class,
        TextNoteEntity::class,
        SessionEntity::class,
        PracticedTabEntity::class,
        TabEntity::class,
        TagEntity::class,
        TabTagCrossRef::class,
        GoalEntity::class
    ],
    version = 17
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioNoteDao(): AudioNoteDao
    abstract fun textNoteDao(): TextNoteDao
    abstract fun sessionDao(): SessionDao
    abstract fun tabDao(): TabDao
    abstract fun goalDao(): GoalDao
}

val Migration16To17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS text_notes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lessonId TEXT NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_text_notes_new_lessonId ON text_notes_new(lessonId)")
        database.execSQL(
            """
            INSERT INTO text_notes_new (id, lessonId, content, createdAt, isFavorite)
            SELECT id, lessonId, content, createdAt, isFavorite
            FROM text_notes
            """.trimIndent()
        )
        database.execSQL("DROP TABLE text_notes")
        database.execSQL("ALTER TABLE text_notes_new RENAME TO text_notes")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audio_notes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lessonId TEXT NOT NULL,
                filePath TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_audio_notes_new_lessonId ON audio_notes_new(lessonId)")
        database.execSQL(
            """
            INSERT INTO audio_notes_new (id, lessonId, filePath, createdAt, isFavorite)
            SELECT id, lessonId, filePath, createdAt, isFavorite
            FROM audio_notes
            """.trimIndent()
        )
        database.execSQL("DROP TABLE audio_notes")
        database.execSQL("ALTER TABLE audio_notes_new RENAME TO audio_notes")
    }
}

val Migration15To16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sessions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO sessions_new (id, startTime, endTime)
            SELECT id, startTime, endTime
            FROM sessions
            """.trimIndent()
        )
        database.execSQL("DROP TABLE sessions")
        database.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

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
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO goals_new (id, syncId, type, description, target, progress, deadline, updatedAt)
            SELECT id, syncId, type, description, target,
                   CASE
                       WHEN type = 'CUSTOM' AND isCompleted = 1 THEN CASE WHEN target > 0 THEN target ELSE 1 END
                       ELSE progress
                   END,
                   deadline, updatedAt
            FROM goals
            """.trimIndent()
        )
        database.execSQL("DROP TABLE goals")
        database.execSQL("ALTER TABLE goals_new RENAME TO goals")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_syncId ON goals(syncId)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tabs_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                lessonNumber INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                isUserTab INTEGER NOT NULL,
                filePath TEXT,
                asciiTabs TEXT,
                folder TEXT NOT NULL,
                openCount INTEGER NOT NULL,
                lastOpenedAt INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                offlineReady INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO tabs_new (
                id, name, description, difficulty, lessonNumber, isCompleted, isUserTab, filePath,
                asciiTabs, folder, openCount, lastOpenedAt, createdAt, updatedAt, offlineReady
            )
            SELECT
                id, name, description, difficulty, lessonNumber, isCompleted, isUserTab, filePath,
                asciiTabs, folder, openCount, lastOpenedAt, createdAt, updatedAt, offlineReady
            FROM tabs
            """.trimIndent()
        )

        database.execSQL("CREATE TABLE IF NOT EXISTS tags (name TEXT NOT NULL PRIMARY KEY)")

        database.query("SELECT id, tagsCsv FROM tabs").use { cursor ->
            val tagsIndex = cursor.getColumnIndexOrThrow("tagsCsv")
            while (cursor.moveToNext()) {
                val normalizedTags = normalizeTags(
                    cursor.getString(tagsIndex)
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                )
                if (normalizedTags.isBlank()) continue
                normalizedTags.split(',').forEach { tag ->
                    database.execSQL("INSERT OR IGNORE INTO tags(name) VALUES (?)", arrayOf(tag))
                }
            }
        }

        database.execSQL("DROP TABLE tabs")
        database.execSQL("ALTER TABLE tabs_new RENAME TO tabs")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tab_tag_cross_ref (
                tabId TEXT NOT NULL,
                tagName TEXT NOT NULL,
                PRIMARY KEY (tabId, tagName),
                FOREIGN KEY (tabId) REFERENCES tabs(id) ON DELETE CASCADE,
                FOREIGN KEY (tagName) REFERENCES tags(name) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_tab_tag_cross_ref_tabId ON tab_tag_cross_ref(tabId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_tab_tag_cross_ref_tagName ON tab_tag_cross_ref(tagName)")
        database.query("SELECT id, tagsCsv FROM tabs").use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val tagsIndex = cursor.getColumnIndexOrThrow("tagsCsv")
            while (cursor.moveToNext()) {
                val tabId = cursor.getString(idIndex)
                val normalizedTags = normalizeTags(
                    cursor.getString(tagsIndex)
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                )
                if (normalizedTags.isBlank()) continue
                normalizedTags.split(',').forEach { tag ->
                    database.execSQL(
                        "INSERT OR IGNORE INTO tab_tag_cross_ref(tabId, tagName) VALUES (?, ?)",
                        arrayOf(tabId, tag)
                    )
                }
            }
        }

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practiced_tabs_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                tabId TEXT NOT NULL,
                duration INTEGER NOT NULL,
                FOREIGN KEY (sessionId) REFERENCES sessions(id) ON DELETE CASCADE,
                FOREIGN KEY (tabId) REFERENCES tabs(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_practiced_tabs_new_sessionId ON practiced_tabs_new(sessionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_practiced_tabs_new_tabId ON practiced_tabs_new(tabId)")
        database.execSQL(
            """
            INSERT INTO practiced_tabs_new (id, sessionId, tabId, duration)
            SELECT id, sessionId, tabId, duration
            FROM practiced_tabs
            WHERE EXISTS (SELECT 1 FROM tabs WHERE tabs.id = practiced_tabs.tabId)
            """.trimIndent()
        )
        database.execSQL("DROP TABLE practiced_tabs")
        database.execSQL("ALTER TABLE practiced_tabs_new RENAME TO practiced_tabs")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS text_notes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lessonId TEXT NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL,
                FOREIGN KEY (lessonId) REFERENCES tabs(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_text_notes_new_lessonId ON text_notes_new(lessonId)")
        database.execSQL(
            """
            INSERT INTO text_notes_new (id, lessonId, content, createdAt, isFavorite)
            SELECT id, lessonId, content, createdAt, isFavorite
            FROM text_notes
            WHERE EXISTS (SELECT 1 FROM tabs WHERE tabs.id = text_notes.lessonId)
            """.trimIndent()
        )
        database.execSQL("DROP TABLE text_notes")
        database.execSQL("ALTER TABLE text_notes_new RENAME TO text_notes")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audio_notes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lessonId TEXT NOT NULL,
                filePath TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL,
                FOREIGN KEY (lessonId) REFERENCES tabs(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_audio_notes_new_lessonId ON audio_notes_new(lessonId)")
        database.execSQL(
            """
            INSERT INTO audio_notes_new (id, lessonId, filePath, createdAt, isFavorite)
            SELECT id, lessonId, filePath, createdAt, isFavorite
            FROM audio_notes
            WHERE EXISTS (SELECT 1 FROM tabs WHERE tabs.id = audio_notes.lessonId)
            """.trimIndent()
        )
        database.execSQL("DROP TABLE audio_notes")
        database.execSQL("ALTER TABLE audio_notes_new RENAME TO audio_notes")
    }
}

val Migration14To15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tabs ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL(
            """
            UPDATE tabs
            SET createdAt = CASE
                WHEN updatedAt > 0 THEN updatedAt
                WHEN lastOpenedAt > 0 THEN lastOpenedAt
                ELSE 0
            END
            """.trimIndent()
        )
    }
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
