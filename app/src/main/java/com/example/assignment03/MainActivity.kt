package com.example.assignment03

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.assignment03.ui.screen.EntryEditorScreen
import com.example.assignment03.ui.theme.MemoTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoTheme {
                EntryEditorScreen()
            }
        }
    }
}
