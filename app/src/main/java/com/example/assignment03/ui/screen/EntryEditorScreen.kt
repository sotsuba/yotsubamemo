
package com.example.assignment03.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.example.assignment03.ui.theme.MemoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MemoDraft(
    val title: String?,
    val content: String,
    val photoUri: String?,
    val mood: String?,
    val createdAt: Long
)

@Composable
fun EntryEditorScreen(
    modifier: Modifier = Modifier,
    photoUri: String? = null,
    onTakePhoto: () -> Unit = {},
    onChoosePhoto: () -> Unit = {},
    onBack: () -> Unit = {},
    onSave: (MemoDraft) -> Unit = {}
) {
    var title by rememberSaveable {
        mutableStateOf("")
    }

    var content by rememberSaveable {
        mutableStateOf("")
    }

    var mood by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val createdAt = remember { System.currentTimeMillis() }

    val date = remember(createdAt) {
        SimpleDateFormat(
            "MMMM d, yyyy",
            Locale.ENGLISH
        ).format(Date(createdAt)).uppercase(Locale.ENGLISH)
    }

    val canSave = photoUri != null ||
            title.isNotBlank() ||
            content.isNotBlank()

    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "New Moment",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.width(48.dp))
            }
        },

        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(
                            MemoDraft(
                                title = title.trim().ifBlank { null },
                                content = content.trim(),
                                photoUri = photoUri,
                                mood = mood,
                                createdAt = createdAt
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Save Moment")
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Photo
            AnimatedVisibility(visible = !isKeyboardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(5f / 4f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        )
                        .clickable { onChoosePhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Journal photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Add a little moment",
                                color = MaterialTheme.colorScheme
                                    .onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Date
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional title
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Title (optional)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Journal content
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = "What would you like to remember?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Optional mood
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How did you feel?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Peaceful", "Happy", "Thoughtful")
                        .forEach { option ->
                            FilterChip(
                                selected = mood == option,
                                onClick = {
                                    mood = if (mood == option) {
                                        null
                                    } else {
                                        option
                                    }
                                },
                                label = { Text(option) }
                            )
                        }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 800
)
@Composable
private fun EntryEditorPreview() {
    MemoTheme(
        darkTheme = true,
        dynamicColor = false
    ) {
        EntryEditorScreen(
            photoUri = null,
            onTakePhoto = {},
            onChoosePhoto = {},
            onBack = {},
            onSave = {}
        )
    }
}
