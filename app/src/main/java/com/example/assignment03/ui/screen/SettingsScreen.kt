package com.example.assignment03.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.assignment03.ui.state.ThemeMode

@Composable
fun SettingsScreen(theme: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(Modifier.fillMaxWidth().selectable(selected = theme == mode,
                onClick = { onThemeChange(mode) }, role = Role.RadioButton).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = theme == mode, onClick = null)
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(start = 12.dp))
            }
        }
        HorizontalDivider()
        Text("About memo.", style = MaterialTheme.typography.titleMedium)
        Text("Version $version")
        Text("A small photo journal for your everyday moments.")
        Text("Your notes and saved photos are stored locally on this device. Memo does not provide cloud sync. Uninstalling the app removes its local data.")
    }
}
