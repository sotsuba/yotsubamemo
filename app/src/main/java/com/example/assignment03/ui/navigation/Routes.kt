package com.example.assignment03.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object JournalRoute

@Serializable
data object SettingsRoute

@Serializable
data class MomentDetailRoute (
    val entryId: Long
)

@Serializable
data class EntryEditorRoute (
    val entryId: Long? = null,
    val draftPhotoPath: String? = null
)
