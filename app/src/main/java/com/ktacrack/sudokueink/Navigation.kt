package com.ktacrack.sudokueink

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Menu : Screen("menu/{mode}") {
        fun createRoute(mode: String = "none") = "menu/$mode"
    }

    object Game : Screen("game/{mode}/{difficulty}") {
        fun createRoute(mode: GameMode, difficulty: Difficulty) =
            "game/${mode.name}/${difficulty.name}"
    }

    object Statistics : Screen("statistics")
}

@Composable
fun AppNavigation(onThemeChange: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Menu.createRoute("none")
    ) {
        // Pantalla de menú amb paràmetre de mode
        composable(
            route = Screen.Menu.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "none"
                }
            )
        ) { backStackEntry ->
            val currentMode = backStackEntry.arguments?.getString("mode") ?: "none"

            MainScreen(
                currentMode = if (currentMode != "none") currentMode else null,
                onModeSelected = { mode ->
                    // Navegar al submenu del mode seleccionat
                    navController.navigate(Screen.Menu.createRoute(mode))
                },
                onGameModeSelected = { mode, difficulty ->
                    // Navegar directament al joc
                    navController.navigate(Screen.Game.createRoute(mode, difficulty))
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                },
                onBackToMainMenu = {
                    // Tornar al menú principal (des del submenu)
                    navController.navigate(Screen.Menu.createRoute("none")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                },
                onThemeChange = onThemeChange
            )
        }

        // Pantalla de joc
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType }
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

            GameScreen(
                difficulty = difficulty,
                mode = mode,
                onBack = {
                    // ✅ Tornar al submenu del mateix mode
                    navController.navigate(Screen.Menu.createRoute(modeName ?: "NORMAL")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla d'estadístiques
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = {
                    // ✅ Tornar al menú principal
                    navController.navigate(Screen.Menu.createRoute("none")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
