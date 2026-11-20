package app.lalla.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.lalla.ui.chat.ChatScreen
import app.lalla.ui.memory.MemoryScreen
import app.lalla.ui.settings.SettingsScreen
import app.lalla.ui.tasks.TasksScreen

object LallaDestinations {
    const val CHAT     = "chat"
    const val MEMORY   = "memory"
    const val SETTINGS = "settings"
    const val TASKS    = "tasks"
}

@Composable
fun LallaNavHost(pendingVoiceCommand: String? = null) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LallaDestinations.CHAT
    ) {
        composable(LallaDestinations.CHAT) {
            ChatScreen(
                navController = navController,
                pendingVoiceCommand = pendingVoiceCommand
            )
        }
        composable(LallaDestinations.MEMORY) {
            MemoryScreen(navController = navController)
        }
        composable(LallaDestinations.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(LallaDestinations.TASKS) {
            TasksScreen(navController = navController)
        }
    }
}
