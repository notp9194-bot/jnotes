package com.notp9194bot.jnotes.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

    NavHost(navController = navController, startDestination = start) {
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
