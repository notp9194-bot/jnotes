package com.notp9194bot.jnotesadmin.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.notp9194bot.jnotesadmin.ui.chat.AdminChatScreen
import com.notp9194bot.jnotesadmin.ui.settings.AdminSettingsScreen
import com.notp9194bot.jnotesadmin.ui.threads.ThreadListScreen

object AdminRoutes {
    const val THREADS = "threads"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{userId}/{name}"
    fun chat(userId: String, name: String) = "chat/$userId/${name.ifBlank { "User" }}"
}

@Composable
fun AdminApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = AdminRoutes.THREADS) {
        composable(AdminRoutes.THREADS) {
            ThreadListScreen(
                onOpenChat = { userId, name -> nav.navigate(AdminRoutes.chat(userId, name)) },
                onOpenSettings = { nav.navigate(AdminRoutes.SETTINGS) },
            )
        }
        composable(AdminRoutes.SETTINGS) {
            AdminSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = AdminRoutes.CHAT,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
            ),
        ) { entry ->
            val uid = entry.arguments?.getString("userId").orEmpty()
            val nm = entry.arguments?.getString("name").orEmpty()
            AdminChatScreen(userId = uid, userName = nm, onBack = { nav.popBackStack() })
        }
    }
}
