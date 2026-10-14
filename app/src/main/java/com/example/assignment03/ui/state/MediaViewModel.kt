package com.example.assignment03.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.assignment03.media.DraftPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

class MediaViewModel(application: Application, private val state: SavedStateHandle) : AndroidViewModel(application) {
    private val photos = DraftPhotos(application)
    val ready = state.getStateFlow<String?>("ready", null)
    val busy = state.getStateFlow("busy", false)
    val error = state.getStateFlow<String?>("error", null)
    val replacesEditor get() = state.get<Boolean>("replacesEditor") == true

    init {
        // Only an external activity can deliver a result after process recreation.
        if (state.get<String>("pendingCamera") == null && state.get<Boolean>("pendingPicker") != true) {
            state["busy"] = false
        }
    }

    fun dismissError() { state["error"] = null }

    fun startCamera(launch: (Uri) -> Unit) {
        if (busy.value) return
        state["busy"] = true
        state["replacesEditor"] = false
        viewModelScope.launch {
            try {
                val name = withContext(Dispatchers.IO) { photos.create() }
                state["pendingCamera"] = name
                launch(photos.cameraUri(name))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed() }
        }
    }

    fun startPicker(replace: Boolean, launch: () -> Unit) {
        if (busy.value) return
        state["busy"] = true
        state["replacesEditor"] = replace
        state["pendingPicker"] = true
        try { launch() } catch (_: Exception) { failed() }
    }

    fun cameraResult(success: Boolean) {
        val name = state.get<String>("pendingCamera")
        state["pendingCamera"] = null
        if (!success || name == null) {
            viewModelScope.launch(Dispatchers.IO) { photos.delete(name) }
            state["busy"] = false
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { photos.validate(name) }
                state["ready"] = name
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                withContext(Dispatchers.IO) { photos.delete(name) }
                state["error"] = "Could not read the captured photo. Please try again."
            } finally { state["busy"] = false }
        }
    }

    fun pickerResult(uri: Uri?) {
        state["pendingPicker"] = false
        if (uri == null) { state["busy"] = false; return }
        viewModelScope.launch {
            try {
                state["ready"] = withContext(Dispatchers.IO) { photos.copy(uri) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state["error"] = "Could not open that image. Please choose another photo." }
            finally { state["busy"] = false }
        }
    }

    private fun failed() {
        val pending = state.get<String>("pendingCamera")
        state["pendingCamera"] = null
        state["pendingPicker"] = false
        state["busy"] = false
        state["error"] = "Camera or image picker is unavailable. Please try again."
        viewModelScope.launch(Dispatchers.IO) { photos.delete(pending) }
    }

    fun consumed() { state["ready"] = null }

    fun cleanOld(activeEditorPhoto: String?) {
        val protected = listOfNotNull(activeEditorPhoto, state["pendingCamera"], ready.value).toSet()
        viewModelScope.launch(Dispatchers.IO) { photos.cleanOld(protected) }
    }
}
