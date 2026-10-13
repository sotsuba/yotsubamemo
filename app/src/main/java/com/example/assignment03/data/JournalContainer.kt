package com.example.assignment03.data

import android.content.Context
import androidx.room.Room

class JournalContainer private constructor(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext, JournalDatabase::class.java, "memo.db"
    ).build()

    val repository = JournalRepository(database.journalDao(), InternalPhotoStore(context))

    companion object {
        @Volatile private var instance: JournalContainer? = null

        fun get(context: Context): JournalContainer = instance ?: synchronized(this) {
            instance ?: JournalContainer(context.applicationContext).also { instance = it }
        }
    }
}
