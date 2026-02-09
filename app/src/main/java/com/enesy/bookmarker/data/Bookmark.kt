package com.enesy.bookmarker.data

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Stable
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val originalTitle: String,
    val platform: String,
    val folderId: Long?,
    val isSummarized: Boolean,
    val tags: String = "",
    val rawContent: String = "",
    val thumbnail: String? = null
)