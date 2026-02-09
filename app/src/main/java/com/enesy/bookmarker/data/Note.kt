package com.enesy.bookmarker.data

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Stable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookmarkId: Long,
    val content: String,
    val createdAt: Long,
    val folderId: Long?,
    val tags: String = "" // Added tags
)