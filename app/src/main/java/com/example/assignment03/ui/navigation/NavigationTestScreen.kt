package com.example.assignment03.ui.navigation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
@Composable
fun NavigationTestScreen() {
    val navController = rememberNavController()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row {
            Button(
                onClick = {
                    navController.navigate(JournalRoute)
                }
            ) {
                Text("Journal")
            }

            Button(
                onClick = {
                    navController.navigate(SettingsRoute)
                }
            ) {
                Text("Settings")
            }

            Button(
                onClick = {
                    navController.navigate(EntryEditorRoute())
                }
            ) {
                Text("Editor")
            }
        }

        AppNavHost(
            navController = navController,
            modifier = Modifier.weight(1f)
        )
    }
}