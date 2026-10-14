package com.example.assignment03.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.navOptions
import com.example.assignment03.ui.components.JournalBottomBar
import com.example.assignment03.ui.components.NavTab
import com.example.assignment03.ui.navigation.*
import com.example.assignment03.ui.state.SettingsViewModel
import com.example.assignment03.ui.state.MediaViewModel

@Composable
fun MainScreen(settings: SettingsViewModel = viewModel()) {
    val navController = rememberNavController()
    val media: MediaViewModel = viewModel()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val editor = backStackEntry?.destination?.hasRoute<EntryEditorRoute>() == true
    val busy by media.busy.collectAsStateWithLifecycle()
    val ready by media.ready.collectAsStateWithLifecycle()
    val error by media.error.collectAsStateWithLifecycle()
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), media::cameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), media::pickerResult)

    LaunchedEffect(ready) {
        ready?.let { path ->
            if (media.replacesEditor && editor) {
                backStackEntry?.savedStateHandle?.set("replacementPhoto", path)
            } else {
                navController.navigate(EntryEditorRoute(draftPhotoPath = path)) { launchSingleTop = true }
            }
            media.consumed()
        }
    }
    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); media.dismissError() }
    }
    LaunchedEffect(backStackEntry?.id) {
        val active = if (editor) backStackEntry?.savedStateHandle?.get<String>("activePhoto")
            ?: backStackEntry?.toRoute<EntryEditorRoute>()?.draftPhotoPath else null
        media.cleanOld(active)
    }

    Scaffold(bottomBar = {
        if (!editor) JournalBottomBar(
            currentTab = if (backStackEntry?.destination?.hasRoute<SettingsRoute>() == true) NavTab.SETTINGS else NavTab.JOURNAL,
            onTabSelected = { tab ->
                if (!busy) when (tab) {
                    NavTab.CAMERA -> media.startCamera { camera.launch(it) }
                    else -> {
                        val options = navOptions {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        if (tab == NavTab.JOURNAL) navController.navigate(JournalRoute, options)
                        else navController.navigate(SettingsRoute, options)
                    }
                }
            }
        )
    }) { padding ->
        AppNavHost(navController, Modifier.padding(padding), importing = busy, settings = settings,
            onChoosePhoto = { replace -> media.startPicker(replace) { picker.launch("image/*") } })
    }
}
