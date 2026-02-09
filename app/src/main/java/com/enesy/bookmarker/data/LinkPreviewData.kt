package com.enesy.bookmarker.data

import androidx.compose.runtime.Stable

@Stable
data class LinkPreviewData(
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val siteName: String?
)
