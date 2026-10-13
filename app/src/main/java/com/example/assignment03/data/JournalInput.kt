package com.example.assignment03.data

import android.net.Uri

enum class JournalMood(val code: String, val label: String) {
    PEACEFUL("peaceful", "Peaceful"),
    HAPPY("happy", "Happy"),
    THOUGHTFUL("thoughtful", "Thoughtful")
}

data class JournalInput(
    val title: String? = null,
    val content: String = "",
    val mood: JournalMood? = null
)

sealed interface PhotoChange {
    data object Keep : PhotoChange
    data object Remove : PhotoChange
    data class Replace(val uri: Uri) : PhotoChange
}
