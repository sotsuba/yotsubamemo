package com.example.assignment03.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.assignment03.model.JournalEntry
import com.example.assignment03.ui.components.JournalGrid
import com.example.assignment03.ui.components.JournalTopBar
import com.example.assignment03.ui.theme.MemoTheme

@Composable
fun JournalScreen(
    entries: List<JournalEntry> = emptyList(),
    loading: Boolean = false,
    error: Boolean = false,
    importing: Boolean = false,
    onRetry: () -> Unit = {},
    onAddPhoto: () -> Unit = {},
    onNewMoment: () -> Unit = {},
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onEntryClick: (Long) -> Unit = {}
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    val normalizedQuery = query.trim()
    val filtered = remember(entries, normalizedQuery) {
        entries.filter { it.title.orEmpty().contains(normalizedQuery, true) || it.content.contains(normalizedQuery, true) }
    }
    Scaffold(
        topBar = {
            Column {
                JournalTopBar(onSearchClick = { searching = !searching; if (!searching) onQueryChange("") })
                if (searching) OutlinedTextField(value = query, onValueChange = onQueryChange,
                    label = { Text("Search moments") }, singleLine = true,
                    trailingIcon = { TextButton(onClick = { onQueryChange("") }) { Text("Clear") } },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { if (!importing) addMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    if (importing) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Default.Add, contentDescription = "Add moment")
                }
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(text = { Text("Write a note") }, onClick = { addMenu = false; onNewMoment() })
                    DropdownMenuItem(text = { Text("Choose photo") }, onClick = { addMenu = false; onAddPhoto() })
                }
            }
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("Could not load your moments.")
                Button(onClick = onRetry) { Text("Retry") }
            }
            entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your moments will appear here. Write a note, take or choose a photo.")
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No matching moments.")
            }
            else -> JournalGrid(entries = filtered, onEntryClick = onEntryClick, modifier = Modifier.padding(padding))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JournalPreview() {
    MemoTheme(true) { JournalScreen(entries = listOf(JournalEntry(id = 1, title = "Rainy afternoon"))) }
}
