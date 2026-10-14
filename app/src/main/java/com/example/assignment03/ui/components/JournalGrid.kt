package com.example.assignment03.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import com.example.assignment03.model.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun JournalGrid(
    entries: List<JournalEntry> = emptyList(),
    modifier: Modifier = Modifier,
    onEntryClick: (Long) -> Unit = {}
) {
    val locale = LocalConfiguration.current.locales[0]
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            JournalCard(
                title = entry.title ?: entry.content.lineSequence().firstOrNull { it.isNotBlank() } ?: "Untitled moment",
                date = SimpleDateFormat("MMM d", locale).format(Date(entry.createdAt)),
                image = {
                    if (entry.photoUri == null) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
                    } else AsyncImage(
                        model = entry.photoUri,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                modifier = Modifier.clickable { onEntryClick(entry.id) }
            )
        }
    }
}