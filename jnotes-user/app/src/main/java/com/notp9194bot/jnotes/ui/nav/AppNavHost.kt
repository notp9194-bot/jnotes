package com.notp9194bot.jnotes.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.ui.about.AboutScreen
import com.notp9194bot.jnotes.ui.calendar.CalendarScreen
import com.notp9194bot.jnotes.ui.chat.ChatScreen
import com.notp9194bot.jnotes.ui.chat.FeedbackScreen
import com.notp9194bot.jnotes.ui.editor.EditorScreen
import com.notp9194bot.jnotes.ui.home.HomeScreen
import com.notp9194bot.jnotes.ui.lock.LockManager
import com.notp9194bot.jnotes.ui.lock.PinLockScreen
import com.notp9194bot.jnotes.ui.settings.SettingsScreen
import com.notp9194bot.jnotes.ui.stats.StatsScreen

@Composable
fun AppNavHost(initialNoteId: String? = null) {
    val context = LocalContext.current
    val settings by ServiceLocator.settings(context).flow.collectAsState(initial = com.notp9194bot.jnotes.data.model.Settings())
    val navController = rememberNavController()

    val pinHash = settings.pinHash
    val needsLock = !pinHash.isNullOrEmpty()
    var unlocked by rememberSaveable { mutableStateOf(false) }

    // ── Re-lock logic on app resume ───────────────────────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, settings.autoLockSeconds, settings.lockOnExit, needsLock) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && needsLock && unlocked) {
                val backgroundedAt = LockManager.consumeBackgroundedAt()
                val elapsed = if (backgroundedAt > 0) System.currentTimeMillis() - backgroundedAt else 0L
                val shouldLock = settings.lockOnExit ||
                    (settings.autoLockSeconds > 0 && elapsed >= settings.autoLockSeconds * 1000L)
                if (shouldLock) {
                    unlocked = false
                    navController.navigate(Routes.LOCK) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val start = when {
        needsLock && !unlocked -> Routes.LOCK
        else -> Routes.HOME
    }

    LaunchedEffect(initialNoteId, start, unlocked) {
        if (initialNoteId != null && (!needsLock || unlocked)) {
            navController.navigate(Routes.editor(initialNoteId)) {
                launchSingleTop = true
            }
        }
    }

    // The shared bottom navigation bar appears on every screen except the
    // PIN lock and the in-place editor (where the toolbar already crowds
    // the chrome). It lets the user jump between sections from anywhere.
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute != null &&
        currentRoute != Routes.LOCK &&
        !currentRoute.startsWith("editor")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNav(
                    current = currentRoute,
                    onSelect = { dest ->
                        navController.navigate(dest) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(outerPadding),
        ) {
            composable(Routes.LOCK) {
                PinLockScreen(
                    pinHash = pinHash.orEmpty(),
                    pinSalt = settings.pinSalt,
                    biometricEnabled = settings.biometricEnabled,
                    onUnlocked = {
                        unlocked = true
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOCK) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                )
            }

            composable(
                route = Routes.EDITOR,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("noteId").orEmpty()
                EditorScreen(
                    noteId = id,
                    onBack = { navController.popBackStack() },
                    onNavigateToNote = { id2 ->
                        navController.navigate(Routes.editor(id2)) { launchSingleTop = true }
                    },
                )
            }

            composable(Routes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                    onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                )
            }

            composable(Routes.CHAT) {
                ChatScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.FEEDBACK) {
                FeedbackScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                )
            }
        }
    }
}

@Composable
private fun BottomNav(current: String?, onSelect: (String) -> Unit) {
    data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)
    val tabs = listOf(
        Tab(Routes.HOME, "Notes") { Icon(Icons.Outlined.Home, null) },
        Tab(Routes.CALENDAR, "Calendar") { Icon(Icons.Outlined.CalendarMonth, null) },
        Tab(Routes.STATS, "Stats") { Icon(Icons.Outlined.QueryStats, null) },
        Tab(Routes.SETTINGS, "Settings") { Icon(Icons.Outlined.Settings, null) },
        Tab(Routes.ABOUT, "About") { Icon(Icons.Outlined.Info, null) },
    )
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { if (current != tab.route) onSelect(tab.route) },
                icon = tab.icon,
                label = { Text(tab.label) },
            )
        }
    }
}
