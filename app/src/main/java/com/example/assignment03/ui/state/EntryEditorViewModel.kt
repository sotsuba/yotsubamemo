package com.example.assignment03.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.assignment03.data.JournalContainer
import com.example.assignment03.data.JournalInput
import com.example.assignment03.data.PhotoChange
import com.example.assignment03.data.JournalMood
import com.example.assignment03.media.DraftPhotos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryEditorViewModel(application: Application, private val state: SavedStateHandle) : AndroidViewModel(application) {
    private val photos = DraftPhotos(application)
    private val repository = JournalContainer.get(application).repository
    val title = state.getStateFlow("title", "")
    val content = state.getStateFlow("content", "")
    val mood = state.getStateFlow<String?>("mood", null)
    val photo = state.getStateFlow<String?>("photo", null)
    val savedId = state.getStateFlow<Long?>("savedId", null)
    val error = state.getStateFlow<String?>("error", null)
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    val originalUri = state.getStateFlow<String?>("originalUri", null)
    val removedPhoto = state.getStateFlow("removedPhoto", false)
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    val initialized = state.getStateFlow("initialized", false)
    val unavailable = state.getStateFlow("unavailable", false)
    val createdAt = state.getStateFlow("createdAt", System.currentTimeMillis())

    fun initialize(path: String?, entryId: Long? = null) {
        if (state.get<Boolean>("initialized") == true) return
        if (loading.value) return
        state["entryId"] = entryId
        if (entryId == null) {
            state["initialized"] = true
            state["photo"] = path
            return
        }
        _loading.value = true
        state["error"] = null
        viewModelScope.launch {
            try {
                val entry = repository.observeEntry(entryId).first()
                if (entry == null) state["unavailable"] = true
                else {
                    state["title"] = entry.title.orEmpty()
                    state["content"] = entry.content
                    state["mood"] = entry.mood
                    state["originalUri"] = entry.photoUri
                    state["createdAt"] = entry.createdAt
                    state["initialTitle"] = entry.title.orEmpty()
                    state["initialContent"] = entry.content
                    state["initialMood"] = entry.mood
                    state["initialized"] = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state["error"] = "Could not load this moment. Please retry." }
            finally { _loading.value = false }
        }
    }
    fun titleChanged(value: String) { if (!_saving.value) state["title"] = value }
    fun contentChanged(value: String) { if (!_saving.value) state["content"] = value }
    fun moodChanged(value: String?) { if (!_saving.value) state["mood"] = value }
    fun photoUri(path: String?): String? = path?.let { photos.localUri(it).toString() }
        ?: originalUri.value.takeUnless { removedPhoto.value }

    fun hasChanges(): Boolean = photo.value != null || (removedPhoto.value && originalUri.value != null) ||
        title.value != state.get<String>("initialTitle").orEmpty() ||
        content.value != state.get<String>("initialContent").orEmpty() ||
        mood.value != state.get<String>("initialMood")

    fun removePhoto() {
        if (_saving.value || loading.value) return
        val old = photo.value
        state["photo"] = null
        state["removedPhoto"] = true
        viewModelScope.launch(Dispatchers.IO) { photos.delete(old) }
    }

    fun replacePhoto(path: String) {
        if (_saving.value || loading.value) return
        val old = photo.value
        state["removedPhoto"] = false
        state["photo"] = path
        if (old != path) viewModelScope.launch(Dispatchers.IO) { photos.delete(old) }
    }

    fun save() {
        if (_saving.value || savedId.value != null || loading.value || unavailable.value || state.get<Boolean>("initialized") != true) return
        _saving.value = true
        state["error"] = null
        viewModelScope.launch {
            try {
                val input = JournalInput(title.value, content.value, JournalMood.entries.find { it.label == mood.value })
                val existingId = state.get<Long>("entryId")
                val id = if (existingId == null) repository.create(input, photo.value?.let(photos::localUri))
                else {
                    val change = photo.value?.let { PhotoChange.Replace(photos.localUri(it)) }
                        ?: if (removedPhoto.value) PhotoChange.Remove else PhotoChange.Keep
                    repository.update(existingId, input, change)
                    existingId
                }
                withContext(NonCancellable + Dispatchers.IO) { photos.delete(photo.value) }
                state["savedId"] = id
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: NoSuchElementException) { state["unavailable"] = true }
            catch (_: Exception) { state["error"] = "Could not save this moment. Your draft is still here. Please try again." }
            finally { _saving.value = false }
        }
    }

    fun discard() {
        val path = photo.value
        // Do not cancel cleanup when this navigation entry is removed.
        photos.delete(path)
    }
}
