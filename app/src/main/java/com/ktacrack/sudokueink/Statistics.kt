package com.ktacrack.sudokueink

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class GameStatistics(
    val gamesCompletedEasy: Int = 0,
    val gamesCompletedMedium: Int = 0,
    val gamesCompletedHard: Int = 0,
    val bestTimeEasy: Int? = null,  // en segons, null si mai ha completat
    val bestTimeMedium: Int? = null,
    val bestTimeHard: Int? = null
)

object StatisticsManager {
    private const val PREFS_NAME = "sudoku_statistics"
    private const val KEY_STATS = "stats"

    fun loadStatistics(context: Context): GameStatistics {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STATS, null) ?: return GameStatistics()
        return try {
            Json.decodeFromString<GameStatistics>(json)
        } catch (e: Exception) {
            GameStatistics()
        }
    }

    fun saveStatistics(context: Context, stats: GameStatistics) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(stats)
        prefs.edit().putString(KEY_STATS, json).apply()
    }

    fun recordCompletion(context: Context, difficulty: Difficulty, timeInSeconds: Int) {
        val stats = loadStatistics(context)

        val newStats = when (difficulty) {
            Difficulty.EASY -> stats.copy(
                gamesCompletedEasy = stats.gamesCompletedEasy + 1,
                bestTimeEasy = minOf(stats.bestTimeEasy ?: Int.MAX_VALUE, timeInSeconds)
                    .takeIf { it != Int.MAX_VALUE }
            )
            Difficulty.MEDIUM -> stats.copy(
                gamesCompletedMedium = stats.gamesCompletedMedium + 1,
                bestTimeMedium = minOf(stats.bestTimeMedium ?: Int.MAX_VALUE, timeInSeconds)
                    .takeIf { it != Int.MAX_VALUE }
            )
            Difficulty.HARD -> stats.copy(
                gamesCompletedHard = stats.gamesCompletedHard + 1,
                bestTimeHard = minOf(stats.bestTimeHard ?: Int.MAX_VALUE, timeInSeconds)
                    .takeIf { it != Int.MAX_VALUE }
            )
        }

        saveStatistics(context, newStats)
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
