package com.example.assignment03.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.assignment03.ui.components.JournalBottomBar
import com.example.assignment03.ui.components.JournalGrid
import com.example.assignment03.ui.components.JournalPreviewEntry
import com.example.assignment03.ui.components.JournalTopBar
import com.example.assignment03.ui.components.NavTab
import com.example.assignment03.ui.theme.MemoTheme

import com.example.assignment03.ui.components.sampleEntries

@Composable
fun JournalScreen(
    entries: List<JournalPreviewEntry> = sampleEntries,
    onEntryClick: (Int) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(NavTab.JOURNAL) }

    Scaffold(
        topBar = {
            JournalTopBar(
                onSearchClick = {},
                onMenuClick = {}
            )
        },
        bottomBar = {
            JournalBottomBar(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        JournalGrid(
            entries = entries,
            onEntryClick = onEntryClick,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun JournalPreview() {
    MemoTheme(true) {
        JournalScreen()
    }
}
