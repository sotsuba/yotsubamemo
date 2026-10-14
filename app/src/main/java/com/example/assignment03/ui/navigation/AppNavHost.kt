package com.example.assignment03.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.assignment03.R
import com.example.assignment03.data.JournalContainer
import com.example.assignment03.model.JournalEntry
import com.example.assignment03.sharing.prepareMomentShare
import com.example.assignment03.ui.screen.EntryEditorScreen
import com.example.assignment03.ui.screen.JournalScreen
import com.example.assignment03.ui.screen.MomentDetailScreen
import com.example.assignment03.ui.screen.SettingsScreen
import com.example.assignment03.ui.state.MomentActionsViewModel
import com.example.assignment03.ui.state.SettingsViewModel
import com.example.assignment03.ui.state.EntryEditorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class ReadState<T>(val value: T? = null, val loading: Boolean = true, val failed: Boolean = false)
private fun <T> Flow<T>.readState(): Flow<ReadState<T>> = map { ReadState(it, loading = false) }
    .catch { if (it is CancellationException) throw it else emit(ReadState(loading = false, failed = true)) }

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    importing: Boolean = false,
    settings: SettingsViewModel = viewModel(),
    onChoosePhoto: (replaceEditor: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.share_moment)
    val repository = remember(context.applicationContext) { JournalContainer.get(context).repository }
    NavHost(navController = navController, startDestination = JournalRoute, modifier = modifier) {
        composable<JournalRoute> { backStackEntry ->
            val query by backStackEntry.savedStateHandle.getStateFlow("query", "").collectAsStateWithLifecycle()
            var retry by remember { mutableIntStateOf(0) }
            val flow = remember(repository, retry) { repository.observeEntries().readState() }
            val state by flow.collectAsStateWithLifecycle(ReadState<List<JournalEntry>>())
            JournalScreen(
                entries = state.value.orEmpty(), loading = state.loading, error = state.failed,
                importing = importing, onRetry = { retry++ }, onAddPhoto = { onChoosePhoto(false) },
                query = query, onQueryChange = { backStackEntry.savedStateHandle["query"] = it },
                onNewMoment = { navController.navigate(EntryEditorRoute()) },
                onEntryClick = { if (!importing) navController.navigate(MomentDetailRoute(it)) }
            )
        }
        composable<SettingsRoute> {
            val theme by settings.theme.collectAsStateWithLifecycle()
            SettingsScreen(theme, settings::setTheme)
        }
        composable<MomentDetailRoute> { backStackEntry ->
            val scope = rememberCoroutineScope()
            var preparingShare by remember { mutableStateOf(false) }
            val actions: MomentActionsViewModel = viewModel()
            val deleting by actions.deleting.collectAsStateWithLifecycle()
            val deleted by actions.deleted.collectAsStateWithLifecycle()
            val deleteError by actions.error.collectAsStateWithLifecycle()
            var confirmDelete by rememberSaveable { mutableStateOf(false) }
            var retry by remember { mutableIntStateOf(0) }
            val route = backStackEntry.toRoute<MomentDetailRoute>()
            val flow = remember(route.entryId, retry) { repository.observeEntry(route.entryId).readState() }
            val state by flow.collectAsStateWithLifecycle(ReadState<JournalEntry?>())
            val entry = state.value
            LaunchedEffect(deleted) {
                if (deleted) navController.navigate(JournalRoute) {
                    popUpTo<JournalRoute>()
                    launchSingleTop = true
                }
            }
            BackHandler(enabled = deleting) {}
            if (confirmDelete) AlertDialog(
                onDismissRequest = { if (!deleting) confirmDelete = false },
                title = { Text("Delete moment?") },
                text = { Text(deleteError ?: "This will permanently delete the note and its photo.") },
                confirmButton = { TextButton(enabled = !deleting, onClick = { actions.delete(route.entryId) }) {
                    Text(if (deleting) "Deleting…" else "Delete")
                } },
                dismissButton = { TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text("Cancel") } }
            )
            if (state.loading || state.failed || entry == null) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    when {
                        state.loading -> CircularProgressIndicator()
                        state.failed -> {
                            Text("Could not load this moment.")
                            Button(onClick = { retry++ }) { Text("Retry") }
                        }
                        else -> Text("This moment no longer exists.")
                    }
                    TextButton(enabled = !deleting, onClick = { navController.popBackStack() }) { Text("Back") }
                }
            } else MomentDetailScreen(
                entry = entry, onBack = { if (!deleting) navController.popBackStack() },
                onEdit = { navController.navigate(EntryEditorRoute(entryId = route.entryId)) },
                onDelete = { confirmDelete = true },
                actionsEnabled = !deleting && !preparingShare && !importing,
                shareEnabled = !preparingShare,
                onShare = {
                    if (!preparingShare) {
                        preparingShare = true
                        scope.launch {
                            try {
                                context.startActivity(Intent.createChooser(prepareMomentShare(context, entry),
                                    shareTitle))
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) {
                                Toast.makeText(context, R.string.share_failed, Toast.LENGTH_LONG).show()
                            } finally { preparingShare = false }
                        }
                    }
                }
            )
        }
        composable<EntryEditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EntryEditorRoute>()
            val editor: EntryEditorViewModel = viewModel()
            val title by editor.title.collectAsStateWithLifecycle()
            val content by editor.content.collectAsStateWithLifecycle()
            val mood by editor.mood.collectAsStateWithLifecycle()
            val photo by editor.photo.collectAsStateWithLifecycle()
            val saving by editor.saving.collectAsStateWithLifecycle()
            val loading by editor.loading.collectAsStateWithLifecycle()
            val initialized by editor.initialized.collectAsStateWithLifecycle()
            val unavailable by editor.unavailable.collectAsStateWithLifecycle()
            val originalUri by editor.originalUri.collectAsStateWithLifecycle()
            val removedPhoto by editor.removedPhoto.collectAsStateWithLifecycle()
            val createdAt by editor.createdAt.collectAsStateWithLifecycle()
            var confirmDiscard by rememberSaveable { mutableStateOf(false) }
            val saved by editor.savedId.collectAsStateWithLifecycle()
            val error by editor.error.collectAsStateWithLifecycle()
            val replacement by backStackEntry.savedStateHandle.getStateFlow<String?>("replacementPhoto", null)
                .collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { editor.initialize(route.draftPhotoPath, route.entryId) }
            LaunchedEffect(photo) { backStackEntry.savedStateHandle["activePhoto"] = photo }
            LaunchedEffect(replacement) {
                replacement?.let {
                    editor.replacePhoto(it)
                    backStackEntry.savedStateHandle["replacementPhoto"] = null
                }
            }
            LaunchedEffect(saved) {
                if (saved != null) {
                    if (route.entryId != null) navController.popBackStack()
                    else navController.navigate(JournalRoute) {
                        popUpTo<JournalRoute>()
                        launchSingleTop = true
                    }
                }
            }
            val leave: () -> Unit = {
                if (!saving && !importing) {
                    if (editor.hasChanges()) confirmDiscard = true
                    else { editor.discard(); navController.popBackStack() }
                }
            }
            BackHandler { leave() }
            if (confirmDiscard) AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                title = { Text("Discard changes?") },
                text = { Text("Your unsaved changes will be lost.") },
                confirmButton = { TextButton(onClick = {
                    editor.discard(); confirmDiscard = false; navController.popBackStack()
                }) { Text("Discard") } },
                dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
            )
            if (loading || unavailable || !initialized) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    if (loading) CircularProgressIndicator()
                    else {
                        Text(if (unavailable) "This moment no longer exists." else error.orEmpty())
                        if (!unavailable) TextButton(onClick = { editor.initialize(route.draftPhotoPath, route.entryId) }) { Text("Retry") }
                    }
                    TextButton(onClick = leave) { Text("Back") }
                }
            } else EntryEditorScreen(
                photoUri = photo?.let { editor.photoUri(it) } ?: originalUri.takeUnless { removedPhoto }, title = title, content = content, mood = mood,
                saving = saving, importing = importing, error = error,
                editing = route.entryId != null, createdAt = createdAt, onRemovePhoto = editor::removePhoto,
                onTitleChange = editor::titleChanged, onContentChange = editor::contentChanged,
                onMoodChange = editor::moodChanged, onChoosePhoto = { onChoosePhoto(true) },
                onBack = leave, onSave = editor::save
            )
        }
    }
}
