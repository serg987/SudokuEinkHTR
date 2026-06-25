package io.github.serg987.sudokueinkhtr

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AchievementManager {

    fun getAllAchievements(stats: GameStatistics, strings: Strings): List<Achievement> = listOf(
        // PRINCIPIANT
        Achievement(
            id = "first_win",
            title = strings.achievementFirstWin,
            description = strings.achievementFirstWinDesc,
            icon = Icons.Default.EmojiEvents,
            targetValue = 1,
            currentValue = stats.totalGamesCompleted
        ),

        // COMPLETAR PARTIDES
        Achievement(
            id = "games_10",
            title = strings.achievement10Games,
            description = strings.achievement10GamesDesc,  // ✅
            icon = Icons.Default.Star,
            targetValue = 10,
            currentValue = stats.totalGamesCompleted
        ),
        Achievement(
            id = "games_50",
            title = strings.achievement50Games,  // ✅
            description = strings.achievement50GamesDesc,  // ✅
            icon = Icons.Default.Stars,
            targetValue = 50,
            currentValue = stats.totalGamesCompleted
        ),
        Achievement(
            id = "games_100",
            title = strings.achievement100Games,  // ✅
            description = strings.achievement100GamesDesc,  // ✅
            icon = Icons.Default.WorkspacePremium,
            targetValue = 100,
            currentValue = stats.totalGamesCompleted
        ),

        // VELOCITAT
        Achievement(
            id = "speed_easy",
            title = strings.achievementSpeedEasy,
            description = strings.achievementSpeedEasyDesc,
            icon = Icons.Default.Timer,
            targetValue = 180,
            currentValue = stats.bestTimeEasyNormal ?: 999,  // ✅ Número alt però raonable
            isTimeBasedReverse = true  // ✅ AFEGIR
        ),

        Achievement(
            id = "speed_hard",
            title = strings.achievementSpeedHard,
            description = strings.achievementSpeedHardDesc,
            icon = Icons.Default.Bolt,
            targetValue = 600,
            currentValue = stats.bestTimeHardNormal ?: 999,
            isTimeBasedReverse = true  // ✅ AFEGIR
        ),

        // DIFICULTAT
        Achievement(
            id = "hard_master",
            title = strings.achievementHardMaster,  // ✅
            description = strings.achievementHardMasterDesc,  // ✅
            icon = Icons.Default.Casino,
            targetValue = 25,
            currentValue = stats.gamesCompletedHardNormal + stats.gamesCompletedHardAttack
        ),

        // MODE ATAC
        Achievement(
            id = "attack_survivor",
            title = strings.achievementAttackSurvivor,  // ✅
            description = strings.achievementAttackSurvivorDesc,  // ✅
            icon = Icons.Default.LocalFireDepartment,
            targetValue = 1,
            currentValue = stats.gamesCompletedUnder10Seconds
        ),

        // SENSE PISTES
        Achievement(
            id = "no_hints",
            title = strings.achievementNoHints,  // ✅
            description = strings.achievementNoHintsDesc,  // ✅
            icon = Icons.Default.Psychology,
            targetValue = 5,
            currentValue = stats.gamesCompletedNoHints
        ),

        // PERFECTE
        Achievement(
            id = "no_errors",
            title = strings.achievementNoErrors,  // ✅
            description = strings.achievementNoErrorsDesc,  // ✅
            icon = Icons.Default.CheckCircle,
            targetValue = 10,
            currentValue = stats.gamesCompletedNoErrors
        ),

        // 7 Sudokus diaris seguits
        Achievement(
            id = "daily_streak_7",
            title = strings.achievementDailyStreak7,
            description = strings.achievementDailyStreak7Desc,
            icon = Icons.Default.LocalFireDepartment,
            targetValue = 7,
            currentValue = stats.dailyStreak
        ),

        // Sudoku diari sense pistes ni errors
        Achievement(
            id = "daily_perfect",
            title = strings.achievementDailyPerfect,
            description = strings.achievementDailyPerfectDesc,
            targetValue = 3,
            currentValue = stats.dailyGamesPerfectNoHintsNoErrors,
            icon = Icons.Default.EmojiEvents
        ),

        // 50 sodokus zen
        Achievement(
            id = "zen_master",
            title = strings.achievementZenMaster,
            description = strings.achievementZenMasterDesc,
            targetValue = 50,
            currentValue = stats.gamesCompletedZen,
            icon = Icons.Default.Psychology
        )

    )

    // GUARDAR/CARREGAR
    fun saveAchievements(context: Context, achievements: List<Achievement>) {
        val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
        val data = achievements.map {
            AchievementData(it.id, it.currentValue, it.isUnlocked, it.unlockedDate)
        }
        prefs.edit().putString("data", Json.encodeToString(data)).apply()
    }

    fun loadAchievements(context: Context): Map<String, AchievementData> {
        val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
        val json = prefs.getString("data", null) ?: return emptyMap()
        return Json.decodeFromString<List<AchievementData>>(json).associateBy { it.id }
    }

    // COMPROVAR NOUS ACHIEVEMENTS
    fun checkAchievements(
        context: Context,
        stats: GameStatistics,
        strings: Strings,
        onNewAchievement: (Achievement) -> Unit
    ) {
        val saved = loadAchievements(context)
        val current = getAllAchievements(stats, strings)

        val updated = current.map { achievement ->
            val wasUnlocked = saved[achievement.id]?.isUnlocked ?: false
            val isNowUnlocked = if (achievement.isTimeBasedReverse) {
                achievement.currentValue < achievement.targetValue
            } else {
                achievement.currentValue >= achievement.targetValue
            }

            if (!wasUnlocked && isNowUnlocked) {
                val unlockedAchievement = achievement.copy(
                    isUnlocked = true,
                    unlockedDate = System.currentTimeMillis()
                )
                onNewAchievement(unlockedAchievement)  // ✅ AFEGIR aquesta línia
                unlockedAchievement
            } else {
                achievement.copy(isUnlocked = wasUnlocked)
            }
        }

        saveAchievements(context, updated)

    }
}
