package com.ktacrack.sudokueink

import android.content.Context
import java.time.LocalDate

object DailySudokuManager {
    private const val PREFS_NAME = "daily_sudoku_prefs"
    private const val KEY_LAST_PLAYED_DATE = "last_played_date"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LAST_STREAK_DATE = "last_streak_date"

    // Obtenir dificultat del dia (rotació automàtica)
    fun getDailyDifficulty(date: LocalDate = LocalDate.now()): Difficulty {
        val daysSinceEpoch = date.toEpochDay()
        return when ((daysSinceEpoch % 3).toInt()) {
            0 -> Difficulty.EASY
            1 -> Difficulty.MEDIUM
            2 -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }
    }

    // Generar sudoku basat en la data actual
    fun generateDailySudoku(date: LocalDate = LocalDate.now()): SudokuGame {
        val difficulty = getDailyDifficulty(date)
        val seed = date.toEpochDay().toInt()  // Seed única per dia
        return SudokuGenerator.generate(difficulty, seed)
    }


    // Comprovar si ja s'ha jugat avui
    fun hasPlayedToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPlayed = prefs.getString(KEY_LAST_PLAYED_DATE, "") ?: ""
        val today = LocalDate.now().toString()
        return lastPlayed == today
    }

    // Marcar com a jugat avui
    fun markDailyAsPlayed(context: Context, timeInSeconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()

        prefs.edit().apply {
            putString(KEY_LAST_PLAYED_DATE, today)

            // Guardar millor temps específic per avui
            val todayBestKey = "best_time_$today"
            val currentBest = prefs.getInt(todayBestKey, Int.MAX_VALUE)
            if (timeInSeconds < currentBest) {
                putInt(todayBestKey, timeInSeconds)
            }

            apply()
        }

        updateStreak(context)
    }

    // Actualitzar streak
    private fun updateStreak(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val lastStreakDate = prefs.getString(KEY_LAST_STREAK_DATE, "")

        if (lastStreakDate.isNullOrEmpty()) {
            prefs.edit().apply {
                putInt(KEY_CURRENT_STREAK, 1)
                putString(KEY_LAST_STREAK_DATE, today.toString())
                apply()
            }
        } else {
            val lastDate = LocalDate.parse(lastStreakDate)
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)

            when (daysDiff.toInt()) {
                0 -> {} // Mateix dia
                1 -> {
                    // Dia consecutiu
                    val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
                    prefs.edit().apply {
                        putInt(KEY_CURRENT_STREAK, currentStreak + 1)
                        putString(KEY_LAST_STREAK_DATE, today.toString())
                        apply()
                    }
                }
                else -> {
                    // Streak trencat
                    prefs.edit().apply {
                        putInt(KEY_CURRENT_STREAK, 1)
                        putString(KEY_LAST_STREAK_DATE, today.toString())
                        apply()
                    }
                }
            }
        }
    }

    // Obtenir streak actual
    fun getCurrentStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    // Obtenir millor temps d'avui
    fun getDailyBestTime(context: Context, date: LocalDate = LocalDate.now()): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateString = date.toString()
        return prefs.getInt("best_time_$dateString", 0)
    }

    // Obtenir data formatada
    fun getTodayFormatted(): String {
        val today = LocalDate.now()
        return "${today.dayOfMonth}/${today.monthValue}/${today.year}"
    }

    // Nom de la dificultat d'avui
    fun getDailyDifficultyName(strings: Strings): String {
        return when (getDailyDifficulty()) {
            Difficulty.EASY -> strings.difficultyEasy
            Difficulty.MEDIUM -> strings.difficultyMedium
            Difficulty.HARD -> strings.difficultyHard
        }
    }

    // NOU: Sincronitzar Daily amb StatisticsManager per Achievements
    fun recordDailyCompletion(context: Context) {
        // 1. Actualitzar streak (ja existeix)
        updateStreak(context)

        // 2. Sincronitzar amb GameStatistics
        val stats = StatisticsManager.loadStatistics(context)
        val currentStreak = getCurrentStreak(context)

        val newStats = stats.copy(
            dailyStreak = currentStreak,                    // ← Streak per Daily Streak achievement
            gamesCompletedZen = if (isTodayZenMode(context)) stats.gamesCompletedZen + 1 else stats.gamesCompletedZen  // ← Zen
        )

        StatisticsManager.saveStatistics(context, newStats)
    }

    // Helper per saber si avui és Zen Mode
    private fun isTodayZenMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("zen_mode_today_${LocalDate.now().toString()}", false)
    }

    // Marcar el dia com a Zen Mode (cridar des de GameScreen)
    fun markTodayAsZenMode(context: Context, isZen: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("zen_mode_today_${LocalDate.now().toString()}", isZen)
            .apply()
    }
}
