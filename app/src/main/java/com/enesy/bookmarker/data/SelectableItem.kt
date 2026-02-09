package com.enesy.bookmarker.data

data class SelectableItem(
    val id: Long,
    val title: String,
    val content: String,
    val type: ItemType,
    var isSelected: Boolean = false
)

enum class ItemType {
    NOTE, BOOKMARK
}