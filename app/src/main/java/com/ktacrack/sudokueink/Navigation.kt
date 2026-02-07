package com.ktacrack.sudokueink

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object Game : Screen("game/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "game/${difficulty.name}"
    }
    object Statistics : Screen("statistics")
}

@Composable
fun AppNavigation(onThemeChange: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Menu.route) {
        composable(Screen.Menu.route) {
            MainScreen(
                onDifficultySelected = { difficulty ->
                    navController.navigate(Screen.Game.createRoute(difficulty))
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                },
                onThemeChange = onThemeChange  // ← Passa el callback
            )
        }

        composable(Screen.Game.route) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty")
            val difficulty = Difficulty.valueOf(difficultyName ?: "EASY")
            GameScreen(
                difficulty = difficulty,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
