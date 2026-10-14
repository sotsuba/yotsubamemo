package com.example.assignment03

import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.assignment03.ui.state.SettingsViewModel
import com.example.assignment03.ui.state.ThemeMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.assignment03.ui.screen.MainScreen
import com.example.assignment03.ui.theme.MemoTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings: SettingsViewModel = viewModel()
            val theme by settings.theme.collectAsStateWithLifecycle()
            MemoTheme(darkTheme = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }) {
                MainScreen(settings)
            }
        }
    }
}