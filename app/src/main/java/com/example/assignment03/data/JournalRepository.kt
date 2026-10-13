package com.example.assignment03.data

import android.net.Uri
import com.example.assignment03.model.JournalEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JournalRepository internal constructor(
    private val dao: JournalDao,
    private val photos: PhotoStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val writes = Mutex()
    private var initialized = false

    /** Safe to call repeatedly; also runs lazily before the first read/write. */
    suspend fun initialize() = withContext(io) {
        writes.withLock { initializeLocked() }
    }

    private suspend fun initializeLocked() {
        if (!initialized) {
            photos.cleanOrphans(dao.photoPaths().toSet())
            initialized = true
        }
    }

    fun observeEntries(): Flow<List<JournalEntry>> = dao.observeAll()
        .onStart { initialize() }
        .map { entries -> entries.map { it.toModel() } }
        .flowOn(io)

    fun observeEntry(id: Long): Flow<JournalEntry?> = dao.observe(id)
        .onStart { initialize() }
        .map { it?.toModel() }
        .flowOn(io)

    suspend fun create(input: JournalInput, photo: Uri? = null): Long = write {
        val normalized = normalize(input, photo != null)
        val path = photo?.let(photos::importPhoto)
        val now = clock()
        try {
            dao.insert(JournalEntryEntity(
                title = normalized.title, content = normalized.content, photoPath = path,
                mood = normalized.mood?.code, createdAt = now, updatedAt = now
            ))
        } catch (error: Exception) {
            path?.let(photos::delete)
            throw error
        }
    }

    suspend fun update(id: Long, input: JournalInput, photo: PhotoChange = PhotoChange.Keep) = write {
        val old = dao.get(id) ?: throw NoSuchElementException("Moment $id does not exist")
        val hasPhoto = when (photo) {
            PhotoChange.Keep -> old.photoPath?.let(photos::uri) != null
            PhotoChange.Remove -> false
            is PhotoChange.Replace -> true
        }
        val normalized = normalize(input, hasPhoto)
        val path = when (photo) {
            PhotoChange.Keep -> old.photoPath
            PhotoChange.Remove -> null
            is PhotoChange.Replace -> photos.importPhoto(photo.uri)
        }
        try {
            check(dao.update(old.copy(
                title = normalized.title, content = normalized.content, photoPath = path,
                mood = normalized.mood?.code, updatedAt = clock()
            )) == 1) { "Moment $id does not exist" }
        } catch (error: Exception) {
            if (photo is PhotoChange.Replace) path?.let(photos::delete)
            throw error
        }
        if (old.photoPath != path) old.photoPath?.let(photos::delete)
    }

    suspend fun delete(id: Long) = write {
        val old = dao.get(id)
        dao.delete(id)
        old?.photoPath?.let(photos::delete)
    }

    private suspend fun <T> write(block: suspend () -> T): T = withContext(io) {
        writes.withLock {
            initializeLocked()
            currentCoroutineContext().ensureActive()
            // Finish file/DB bookkeeping even if the caller leaves its screen during a write.
            withContext(NonCancellable) { block() }
        }
    }

    private fun normalize(input: JournalInput, hasPhoto: Boolean): JournalInput {
        val normalized = input.copy(title = input.title?.trim()?.ifBlank { null }, content = input.content.trim())
        require(normalized.title != null || normalized.content.isNotEmpty() || hasPhoto) { "Moment is empty" }
        return normalized
    }

    private fun JournalEntryEntity.toModel() = JournalEntry(
        id = id, title = title, content = content, photoUri = photoPath?.let(photos::uri),
        mood = JournalMood.entries.find { it.code == mood }?.label,
        createdAt = createdAt, updatedAt = updatedAt
    )
}
