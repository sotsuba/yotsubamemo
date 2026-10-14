package com.example.assignment03.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("memo_settings", 0)
    private val _theme = MutableStateFlow(
        ThemeMode.entries.find { it.name == preferences.getString("theme", null) } ?: ThemeMode.SYSTEM
    )
    val theme = _theme.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        preferences.edit().putString("theme", mode.name).apply()
        _theme.value = mode
    }
}
