package io.github.serg987.sudokueinkhtr

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Menu : Screen("menu") {
        fun createRoute() = "menu"
    }
    object Game : Screen("game/{mode}/{difficulty}/{isZenMode}") {
        fun createRoute(mode: GameMode, difficulty: Difficulty, isZenMode: Boolean = false) =
            "game/${mode.name}/${difficulty.name}/$isZenMode"
    }
    object DailyMenu : Screen("daily_menu")  // Submenu dificultats
    object DailyGame : Screen("daily_game/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "daily_game/${difficulty.name}"
    }
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")

}


@Composable
fun AppNavigation(onThemeChange: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Menu.createRoute()
    ) {
        // Pantalla de menú principal
        composable(route = Screen.Menu.route) {
            MainScreen(
                onGameModeSelected = { mode, difficulty, isZenMode ->
                    navController.navigate(Screen.Game.createRoute(mode, difficulty, isZenMode))
                },
                onDailySudokuClick = {  // ← AFEGIT AQUÍ DINS
                    navController.navigate(Screen.DailyGame.route)
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                },
                onAchievementsClick = { navController.navigate("achievements") },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onBackToMainMenu = {
                    navController.navigate(Screen.Menu.createRoute()) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                },
                onThemeChange = onThemeChange
            )
        }
        // Daily Sudoku
        composable(Screen.DailyGame.route) {
            DailyGameScreen(
                onBack = {
                    navController.navigate(Screen.Menu.createRoute()) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de joc
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("isZenMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val modeName = backStackEntry.arguments?.getString("mode")
            val mode = try {
                GameMode.valueOf(modeName ?: "NORMAL")
            } catch (e: Exception) {
                GameMode.NORMAL
            }

            val difficultyName = backStackEntry.arguments?.getString("difficulty")
            val difficulty = Difficulty.valueOf(difficultyName ?: "EASY")

            val isZenMode = backStackEntry.arguments?.getBoolean("isZenMode") ?: false

            GameScreen(
                difficulty = difficulty,
                mode = mode,
                isZenMode = isZenMode,
                onBack = {
                    navController.navigate(Screen.Menu.createRoute()) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla d'estadístiques
        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        // Pantalla Achievements
        composable("achievements") {
            AchievementsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Pantalla Configuració
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onThemeChange = onThemeChange
            )
        }
    }
}
