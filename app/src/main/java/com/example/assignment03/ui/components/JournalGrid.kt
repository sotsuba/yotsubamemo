package com.example.assignment03.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.assignment03.R

data class JournalPreviewEntry(
    val id: Int,
    val title: String,
    val date: String,
    @field:DrawableRes val imageRes: Int
)

val sampleEntries = listOf(
    JournalPreviewEntry(
        1, "Rainy afternoon", "Oct 12",
        R.drawable.sample_rainy
    ),
    JournalPreviewEntry(
        2, "A little escape", "Oct 10",
        R.drawable.sample_beach
    ),
    JournalPreviewEntry(
        3, "Lost in books", "Oct 8",
        R.drawable.sample_chill
    )
)

@Composable
fun JournalGrid(
    entries: List<JournalPreviewEntry> = sampleEntries,
    modifier: Modifier = Modifier,
    onEntryClick: (Int) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            JournalCard(
                title = entry.title,
                date = entry.date,
                image = {
                    Image(
                        painter = painterResource(id = entry.imageRes),
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