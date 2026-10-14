package com.example.assignment03.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.assignment03.data.JournalContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Keeps an in-flight deletion alive when the Activity is recreated. */
class MomentActionsViewModel(application: Application, private val state: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = JournalContainer.get(application).repository
    private val _deleting = MutableStateFlow(false)
    val deleting = _deleting.asStateFlow()
    val deleted = state.getStateFlow("deleted", false)
    val error = state.getStateFlow<String?>("deleteError", null)

    fun delete(id: Long) {
        if (_deleting.value || deleted.value) return
        _deleting.value = true
        state["deleteError"] = null
        viewModelScope.launch {
            try {
                repository.delete(id)
                state["deleted"] = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state["deleteError"] = "Could not delete this moment. Please retry." }
            finally { _deleting.value = false }
        }
    }
}
