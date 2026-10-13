package com.example.assignment03.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.assignment03.model.JournalEntry
import com.example.assignment03.ui.components.MomentDetailTopBar
import com.example.assignment03.ui.theme.MemoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MomentDetailScreen(
    entry: JournalEntry,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    shareEnabled: Boolean = true
) {
    val dateText = remember(entry.createdAt) {
        SimpleDateFormat(
            "MMMM d, yyyy",
            Locale.ENGLISH
        ).format(Date(entry.createdAt)).uppercase(Locale.ENGLISH)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MomentDetailTopBar(
                onBack = onBack,
                onEdit = onEdit,
                onShare = onShare,
                shareEnabled = shareEnabled
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo if available
            if (!entry.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = entry.photoUri,
                    contentDescription = entry.title ?: "Moment photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(5f / 4f)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Date
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Title if available
            if (!entry.title.isNullOrBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Content
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mood if available
            if (!entry.mood.isNullOrBlank()) {
                AssistChip(
                    onClick = {},
                    label = { Text(text = entry.mood) }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MomentDetailPreview() {
    MemoTheme {
        MomentDetailScreen(
            entry = JournalEntry(
                id = 1L,
                title = "Rainy afternoon",
                content = "Spent a peaceful afternoon reading by the window while listening to rain drops.",
                mood = "Peaceful"
            ),
            onBack = {},
            onEdit = {},
            onShare = {}
        )
    }
}
