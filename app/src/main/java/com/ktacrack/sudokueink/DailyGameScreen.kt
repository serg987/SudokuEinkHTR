package com.ktacrack.sudokueink

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun DailyGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val difficulty = remember { DailySudokuManager.getDailyDifficulty() }
    val dailyGame = remember { DailySudokuManager.generateDailySudoku() }

    GameScreen(
        difficulty = difficulty,
        mode = GameMode.NORMAL,
        isZenMode = false,
        isDaily = true,
        dailySudoku = dailyGame,
        onBack = onBack,
        onDailyComplete = { timeInSeconds ->
            DailySudokuManager.markDailyAsPlayed(context, timeInSeconds)
        }
    )
}

// Funció auxiliar per formatar temps
fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
