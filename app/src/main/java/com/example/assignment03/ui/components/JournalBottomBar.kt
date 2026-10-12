package com.example.assignment03.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.assignment03.ui.theme.MemoTheme

enum class NavTab {
    JOURNAL, CAMERA, SETTINGS
}

@Composable
fun JournalBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelected(NavTab.JOURNAL) }
                ) {
                    Icon(
                        imageVector = if (currentTab == NavTab.JOURNAL) Icons.Default.Book else Icons.Outlined.Book,
                        contentDescription = "Journal",
                        tint = if (currentTab == NavTab.JOURNAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Journal",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentTab == NavTab.JOURNAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelected(NavTab.SETTINGS) }
                ) {
                    Icon(
                        imageVector = if (currentTab == NavTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = if (currentTab == NavTab.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentTab == NavTab.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { onTabSelected(NavTab.CAMERA) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JournalBottomBarPreview() {
    MemoTheme {
        JournalBottomBar(
            currentTab = NavTab.JOURNAL,
            onTabSelected = {}
        )
    }
}
