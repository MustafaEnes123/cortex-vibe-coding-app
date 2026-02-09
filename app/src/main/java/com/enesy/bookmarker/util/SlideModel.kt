package com.enesy.bookmarker.util

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class SlideModel(
    val type: String,
    val title: String,
    val body: List<String>
)
