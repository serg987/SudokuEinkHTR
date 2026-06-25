package io.github.serg987.sudokueinkhtr

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GameStatistics(
    // Joc Normal
    val gamesCompletedEasyNormal: Int = 0,
    val gamesCompletedMediumNormal: Int = 0,
    val gamesCompletedHardNormal: Int = 0,
    val bestTimeEasyNormal: Int? = null,
    val bestTimeMediumNormal: Int? = null,
    val bestTimeHardNormal: Int? = null,

    // Sudoku Atac
    val gamesCompletedEasyAttack: Int = 0,
    val gamesCompletedMediumAttack: Int = 0,
    val gamesCompletedHardAttack: Int = 0,
    val bestTimeEasyAttack: Int? = null,
    val bestTimeMediumAttack: Int? = null,
    val bestTimeHardAttack: Int? = null,

    val gamesCompletedUnder10Seconds: Int = 0,
    val gamesCompletedNoHints: Int = 0,
    val gamesCompletedNoErrors: Int = 0,

    val dailyStreak: Int = 0,
    val dailyGamesPerfectNoHintsNoErrors: Int = 0,
    val gamesCompletedZen: Int = 0,
){
    val totalGamesCompleted: Int
        get() = gamesCompletedEasyNormal + gamesCompletedMediumNormal +
                gamesCompletedHardNormal + gamesCompletedEasyAttack +
                gamesCompletedMediumAttack + gamesCompletedHardAttack
}

object StatisticsManager {
    private const val PREFS_NAME = "sudoku_statistics"
    private const val KEY_STATS = "stats"

    // ✅ NOU: Obtenir el límit de temps segons dificultat (Mode Atac)
    fun getTimeLimitForDifficulty(difficulty: Difficulty): Int {
        return when (difficulty) {
            Difficulty.EASY -> 20 * 60    // 20 minuts
            Difficulty.MEDIUM -> 30 * 60  // 30 minuts
            Difficulty.HARD -> 45 * 60    // 45 minuts
        }
    }

    // ✅ NOU: Calcular temps restant per Mode Atac
    fun getRemainingTime(difficulty: Difficulty, timeUsed: Int): Int {
        val limit = getTimeLimitForDifficulty(difficulty)
        return (limit - timeUsed).coerceAtLeast(0)
    }

    fun loadStatistics(context: Context): GameStatistics {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STATS, null) ?: return GameStatistics()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            GameStatistics()
        }
    }

    fun saveStatistics(context: Context, stats: GameStatistics) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(stats)
        prefs.edit().putString(KEY_STATS, json).apply()
    }

    fun recordCompletion(
        context: Context,
        difficulty: Difficulty,
        mode: GameMode,
        timeInSeconds: Int,
        hintsUsed: Int = 0,
        errorsCount: Int = 0,
        isDaily: Boolean = false,
        isZenMode: Boolean = false
    ) {
        val stats = loadStatistics(context)

        // 🔥 PAS 1: NOMÉS NORMAL/ATTACK (NO Daily/Zen) → Fites generals
        val normalStats = if (!isDaily && !isZenMode) {
            when (mode) {
                GameMode.NORMAL -> when (difficulty) {
                    Difficulty.EASY -> stats.copy(
                        gamesCompletedEasyNormal = stats.gamesCompletedEasyNormal + 1,
                        bestTimeEasyNormal = minOf(
                            stats.bestTimeEasyNormal ?: Int.MAX_VALUE,
                            timeInSeconds
                        ).takeIf { it != Int.MAX_VALUE },
                        gamesCompletedNoHints = if (hintsUsed == 0) stats.gamesCompletedNoHints + 1 else stats.gamesCompletedNoHints,
                        gamesCompletedNoErrors = if (errorsCount == 0) stats.gamesCompletedNoErrors + 1 else stats.gamesCompletedNoErrors
                    )
                    Difficulty.MEDIUM -> stats.copy(
                        gamesCompletedMediumNormal = stats.gamesCompletedMediumNormal + 1,
                        bestTimeMediumNormal = minOf(
                            stats.bestTimeMediumNormal ?: Int.MAX_VALUE,
                            timeInSeconds
                        ).takeIf { it != Int.MAX_VALUE },
                        gamesCompletedNoHints = if (hintsUsed == 0) stats.gamesCompletedNoHints + 1 else stats.gamesCompletedNoHints,
                        gamesCompletedNoErrors = if (errorsCount == 0) stats.gamesCompletedNoErrors + 1 else stats.gamesCompletedNoErrors
                    )
                    Difficulty.HARD -> stats.copy(
                        gamesCompletedHardNormal = stats.gamesCompletedHardNormal + 1,
                        bestTimeHardNormal = minOf(
                            stats.bestTimeHardNormal ?: Int.MAX_VALUE,
                            timeInSeconds
                        ).takeIf { it != Int.MAX_VALUE },
                        gamesCompletedNoHints = if (hintsUsed == 0) stats.gamesCompletedNoHints + 1 else stats.gamesCompletedNoHints,
                        gamesCompletedNoErrors = if (errorsCount == 0) stats.gamesCompletedNoErrors + 1 else stats.gamesCompletedNoErrors
                    )
                }
                GameMode.ATTACK -> when (difficulty) {
                    Difficulty.EASY -> stats.copy(
                        gamesCompletedEasyAttack = stats.gamesCompletedEasyAttack + 1,
                        bestTimeEasyAttack = maxOf(
                            stats.bestTimeEasyAttack ?: 0,
                            getRemainingTime(Difficulty.EASY, timeInSeconds)
                        ),
                        gamesCompletedUnder10Seconds = if (timeInSeconds < 10) stats.gamesCompletedUnder10Seconds + 1 else stats.gamesCompletedUnder10Seconds,
                        gamesCompletedNoHints = if (hintsUsed == 0) stats.gamesCompletedNoHints + 1 else stats.gamesCompletedNoHints,
                        gamesCompletedNoErrors = if (errorsCount == 0) stats.gamesCompletedNoErrors + 1 else stats.gamesCompletedNoErrors
                    )
                    Difficulty.MEDIUM -> stats.copy(
                        gamesCompletedMediumAttack = stats.gamesCompletedMediumAttack + 1,
                        bestTimeMediumAttack = maxOf(
                            stats.bestTimeMediumAttack ?: 0,
                            getRemainingTime(Difficulty.MEDIUM, timeInSeconds)
                        ),
                        gamesCompletedUnder10Seconds = if (timeInSeconds < 10) stats.gamesCompletedUnder10Seconds + 1 else stats.gamesCompletedUnder10Seconds,
                        gamesCompletedNoHints = if (hintsUsed == 0) stats.gamesCompletedNoHints + 1 else stats.gamesCompletedNoHints,
                        gamesCompletedNoErrors = if (errorsCount == 0) stats.gamesCompletedNoErrors + 1 else stats.gamesCompletedNoErrors
                    )
                    Difficulty.HARD -> stats.copy(
                        gamesCompletedHardAttack = stats.gamesCompletedHardAttack + 1,
                        bestTimeHardAttack = maxOf(
                            stats.bestTimeHardAttack ?: 0,
                            getRemainingTime(Difficulty.HARD, timeInSeconds)
                        ),
                        gamesCompletedUnder10Seconds = if (timeInSeconds < 10) stats.gamesCompletedUnder10Seconds + 1 else stats.gamesCompletedUnder10Seconds,
                        gamesCompletedNoHints = if (hintsUsed == 0) stats.gamesCompletedNoHints + 1 else stats.gamesCompletedNoHints,
                        gamesCompletedNoErrors = if (errorsCount == 0) stats.gamesCompletedNoErrors + 1 else stats.gamesCompletedNoErrors
                    )
                }
            }
        } else {
            // No tocar res si és Daily o Zen
            stats
        }

        // 🔥 PAS 2: NOMÉS ZEN → gamesCompletedZen
        val zenStats = if (isZenMode && !isDaily) {
            normalStats.copy(
                gamesCompletedZen = normalStats.gamesCompletedZen + 1
            )
        } else {
            normalStats
        }

        // 🔥 PAS 3: NOMÉS DAILY → dailyStreak + dailyPerfect
        val finalStats = if (isDaily) {
            zenStats.copy(
                dailyStreak = DailySudokuManager.getCurrentStreak(context),
                dailyGamesPerfectNoHintsNoErrors = if (hintsUsed == 0 && errorsCount == 0)
                    zenStats.dailyGamesPerfectNoHintsNoErrors + 1
                else zenStats.dailyGamesPerfectNoHintsNoErrors
            )
        } else {
            zenStats
        }

        saveStatistics(context, finalStats)
    }


    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }
}