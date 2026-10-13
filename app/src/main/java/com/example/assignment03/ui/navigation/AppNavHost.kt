package com.example.assignment03.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.assignment03.model.JournalEntry
import com.example.assignment03.R
import com.example.assignment03.sharing.prepareMomentShare
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.assignment03.ui.components.sampleEntries
import com.example.assignment03.ui.screen.EntryEditorScreen
import com.example.assignment03.ui.screen.JournalScreen
import com.example.assignment03.ui.screen.MomentDetailScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = JournalRoute, modifier = modifier) {
        composable<JournalRoute> {
            JournalScreen(onEntryClick = { navController.navigate(MomentDetailRoute(it)) })
        }

        composable<SettingsRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        composable<MomentDetailRoute> { backStackEntry ->
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var preparingShare by remember { mutableStateOf(false) }
            val route = backStackEntry.toRoute<MomentDetailRoute>()
            val sample = sampleEntries.find { it.id == route.entryId }
            val journalEntry = JournalEntry(
                id = route.entryId.toLong(),
                title = sample?.title ?: "Moment #${route.entryId}",
                content = "Details for moment #${route.entryId}.",
                photoUri = sample?.let { "android.resource://${context.packageName}/${it.imageRes}" }
            )
            MomentDetailScreen(
                entry = journalEntry,
                onBack = { navController.popBackStack() },
                onEdit = {},
                shareEnabled = !preparingShare,
                onShare = {
                    if (!preparingShare) {
                        preparingShare = true
                        scope.launch {
                            try {
                                val shareIntent = prepareMomentShare(context, journalEntry)
                                context.startActivity(Intent.createChooser(
                                    shareIntent, context.getString(R.string.share_moment)
                                ))
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                Toast.makeText(context, R.string.share_failed, Toast.LENGTH_LONG).show()
                            } finally {
                                preparingShare = false
                            }
                        }
                    }
                }
            )
        }

        composable<EntryEditorRoute> {
            EntryEditorScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
    }
}
