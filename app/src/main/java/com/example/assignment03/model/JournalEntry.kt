package com.example.assignment03.model

data class JournalEntry(
    val id: Long = 0L,
    val title: String? = null,
    val content: String = "",
    val photoUri: String? = null,
    val mood: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
