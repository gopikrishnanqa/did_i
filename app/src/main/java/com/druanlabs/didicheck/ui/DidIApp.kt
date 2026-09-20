package com.druanlabs.didicheck.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.druanlabs.didicheck.ui.before.ChecklistRoute
import com.druanlabs.didicheck.ui.before.RoutinePickerRoute
import com.druanlabs.didicheck.ui.didi.DidIDoItRoute
import com.druanlabs.didicheck.ui.history.HistoryRoute
import com.druanlabs.didicheck.ui.home.HomeRoute
import com.druanlabs.didicheck.ui.settings.EditRoutineRoute
import com.druanlabs.didicheck.ui.settings.ManageActionsRoute
import com.druanlabs.didicheck.ui.settings.ManageRoutinesRoute
import com.druanlabs.didicheck.ui.settings.SettingsRoute
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DidIApp(startDestination: String = Routes.Home) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home),
        Tab("history", "History", Icons.Outlined.History),
        Tab("settings", "Settings", Icons.Outlined.Settings),
    )
    val showTabs = currentRoute == Routes.Home ||
        currentRoute?.startsWith("history") == true ||
        currentRoute == Routes.Settings

    LaunchedEffect(startDestination) {
        if (startDestination != Routes.Home) {
            navController.navigate(startDestination) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showTabs) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = when (tab.route) {
                            "history" -> currentRoute?.startsWith("history") == true
                            else -> currentRoute == tab.route
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val dest = when (tab.route) {
                                    "history" -> Routes.history()
                                    else -> tab.route
                                }
                                navController.navigate(dest) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Home) {
                HomeRoute(
                    onStartCheck = { id -> navController.navigate(Routes.checklist(id)) },
                    onChooseRoutine = { navController.navigate(Routes.Routines) },
                    onMarkSomething = { navController.navigate(Routes.DidIDoIt) },
                    onOpenHistory = {
                        navController.navigate(Routes.history()) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenCompletion = { completion ->
                        navController.navigate(Routes.history(completion.id)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.History,
                arguments = listOf(
                    navArgument("openId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val openId = entry.arguments?.getString("openId").orEmpty().ifBlank { null }
                HistoryRoute(openCompletionId = openId)
            }
            composable(Routes.Settings) {
                SettingsRoute(
                    onManageRoutines = { navController.navigate(Routes.ManageRoutines) },
                    onManageActions = { navController.navigate(Routes.ManageActions) },
                )
            }
            composable(Routes.Routines) {
                RoutinePickerRoute(
                    onBack = { navController.popBackStack() },
                    onOpenRoutine = { id -> navController.navigate(Routes.checklist(id)) },
                    onCreateRoutine = { navController.navigate(Routes.editRoutine()) },
                )
            }
            composable(
                route = Routes.Checklist,
                arguments = listOf(navArgument("routineId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("routineId").orEmpty()
                ChecklistRoute(
                    routineId = id,
                    onBack = { navController.popBackStack() },
                    onChangeRoutine = {
                        navController.navigate(Routes.Routines) {
                            popUpTo(Routes.Home)
                        }
                    },
                    onCompleted = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                        navController.popBackStack(Routes.Home, inclusive = false)
                    },
                )
            }
            composable(Routes.DidIDoIt) {
                DidIDoItRoute(
                    onBack = { navController.popBackStack() },
                    onManageActions = { navController.navigate(Routes.ManageActions) },
                    onStartRoutine = { id -> navController.navigate(Routes.checklist(id)) },
                    onChooseRoutine = { navController.navigate(Routes.Routines) },
                    onCreateRoutine = { navController.navigate(Routes.editRoutine()) },
                )
            }
            composable(Routes.ManageRoutines) {
                ManageRoutinesRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.editRoutine(id)) },
                    onCreate = { navController.navigate(Routes.editRoutine()) },
                )
            }
            composable(
                route = Routes.EditRoutine,
                arguments = listOf(navArgument("routineId") { type = NavType.StringType }),
            ) { entry ->
                EditRoutineRoute(
                    routineId = entry.arguments?.getString("routineId"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ManageActions) {
                ManageActionsRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
