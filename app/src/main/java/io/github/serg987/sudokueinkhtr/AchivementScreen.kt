package io.github.serg987.sudokueinkhtr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()
    val stats = remember { StatisticsManager.loadStatistics(context) }

    // CARREGAR achievements amb estat guardat
    val savedAchievements = remember { AchievementManager.loadAchievements(context) }
    val achievements = remember(strings, savedAchievements) {
        AchievementManager.getAllAchievements(stats, strings).map { achievement ->
            val savedData = savedAchievements[achievement.id]
            achievement.copy(
                isUnlocked = savedData?.isUnlocked
                    ?: if (achievement.isTimeBasedReverse) {
                        achievement.currentValue < achievement.targetValue
                    } else {
                        achievement.currentValue >= achievement.targetValue
                    },
                unlockedDate = savedData?.unlockedDate
            )
        }
    }

    val scale = AdaptiveSizes.getScaleFactor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((16 * scale).dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height((46 * scale).dp))

        // Títol
        Text(
            text = strings.achievements,
            fontSize = (32 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // Progrés total
        val unlocked = achievements.count { it.isUnlocked }
        Text(
            text = "$unlocked / ${achievements.size} ${strings.achievementsUnlocked}",
            fontSize = (20 * scale).sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height((24 * scale).dp))

        // Llista achievements
        achievements.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
            ) {
                rowItems.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height((12 * scale).dp))
        }

        // Espai final
        Spacer(modifier = Modifier.height((90 * scale).dp))
    }
}

@Composable
fun AchievementCard(achievement: Achievement, scale: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            (2 * scale).dp,
            Color.Black
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((8 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                modifier = Modifier.size((36 * scale).dp),
                tint = Color.Black
            )

            Spacer(modifier = Modifier.width((8 * scale).dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = (18 * scale).sp
                )

                Text(
                    text = achievement.description,
                    fontSize = (14 * scale).sp,
                    color = Color.Black,
                    lineHeight = (16 * scale).sp
                )

                if (!achievement.isUnlocked || achievement.isTimeBasedReverse) {
                    val displayProgress = if (achievement.isTimeBasedReverse) {
                        if (achievement.currentValue >= 999) 0f
                        else {
                            if (achievement.isUnlocked) 1f
                            else (achievement.targetValue.toFloat() - achievement.currentValue) / achievement.targetValue
                        }
                    } else {
                        achievement.progress
                    }

                    if (!achievement.isUnlocked) {
                        LinearProgressIndicator(
                            progress = { displayProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = (2 * scale).dp),
                            color = Color.Black,
                            trackColor = Color.White
                        )
                    }

                    val displayText = if (achievement.isTimeBasedReverse) {
                        val currentTime = if (achievement.currentValue >= 999) "--:--"
                        else StatisticsManager.formatTime(achievement.currentValue)
                        val targetTime = StatisticsManager.formatTime(achievement.targetValue)
                        if (achievement.isUnlocked) {
                            "✓ $currentTime / $targetTime"
                        } else {
                            "$currentTime / $targetTime"
                        }
                    } else {
                        if (achievement.isUnlocked) {
                            "✓ ${achievement.currentValue} / ${achievement.targetValue}"
                        } else {
                            "${achievement.currentValue} / ${achievement.targetValue}"
                        }
                    }

                    Text(
                        text = displayText,
                        fontSize = (14 * scale).sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

