package com.example.assignment03.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.assignment03.ui.components.JournalBottomBar
import com.example.assignment03.ui.components.NavTab
import com.example.assignment03.ui.navigation.AppNavHost
import com.example.assignment03.ui.navigation.EntryEditorRoute
import com.example.assignment03.ui.navigation.JournalRoute
import com.example.assignment03.ui.navigation.SettingsRoute

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val backStackEntry by navController
        .currentBackStackEntryAsState()

    val currentDestination = backStackEntry?.destination

    val currentTab = when {
        currentDestination?.hasRoute<SettingsRoute>() == true -> NavTab.SETTINGS
        currentDestination?.hasRoute<EntryEditorRoute>() == true -> NavTab.CAMERA
        else -> NavTab.JOURNAL
    }

    Scaffold(
        bottomBar = {
            JournalBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.JOURNAL -> {
                            navController.navigate(JournalRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        NavTab.SETTINGS -> {
                            navController.navigate(SettingsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        NavTab.CAMERA -> {
                            navController.navigate(EntryEditorRoute()) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    }
            )
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}