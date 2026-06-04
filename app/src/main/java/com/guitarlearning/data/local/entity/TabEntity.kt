package com.guitarlearning.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded
import com.guitarlearning.domain.model.DEFAULT_TAB_FOLDER_KEY
import com.guitarlearning.domain.model.Difficulty

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val lessonNumber: Int,
    val isCompleted: Boolean = false,
    val isUserTab: Boolean = false,
    val filePath: String? = null,
    val asciiTabs: String? = null,
    val folder: String = DEFAULT_TAB_FOLDER_KEY,
    val openCount: Int = 0,
    val lastOpenedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val offlineReady: Boolean = false
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val name: String
)

@Entity(
    tableName = "tab_tag_cross_ref",
    primaryKeys = ["tabId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = TabEntity::class,
            parentColumns = ["id"],
            childColumns = ["tabId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["name"],
            childColumns = ["tagName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tabId"), Index("tagName")]
)
data class TabTagCrossRef(
    val tabId: String,
    val tagName: String
)

data class TabWithTags(
    @Embedded
    val tab: TabEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            value = TabTagCrossRef::class,
            parentColumn = "tabId",
            entityColumn = "tagName"
        )
    )
    val tags: List<TagEntity>
)
