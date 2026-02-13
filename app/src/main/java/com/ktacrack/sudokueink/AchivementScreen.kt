package com.ktacrack.sudokueink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

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
        // Botó tornar
        Spacer(modifier = Modifier.height((46 * scale).dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .height((50 * scale).dp)
                .width((160 * scale).dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(strings.back, fontSize = (24 * scale).sp)
        }

        Spacer(modifier = Modifier.height((16 * scale).dp))

        // Títol
        Text(
            text = strings.achievements,
            fontSize = (32 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface  // ✅ Color adaptatiu
        )

        // Progrés total
        val unlocked = achievements.count { it.isUnlocked }
        Text(
            text = "$unlocked / ${achievements.size} ${strings.achievementsUnlocked}",
            fontSize = (20 * scale).sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height((24 * scale).dp))

        // Llista achievements
        achievements.forEach { achievement ->
            AchievementCard(achievement, scale, strings)
            Spacer(modifier = Modifier.height((12 * scale).dp))
        }

        // Espai final
        Spacer(modifier = Modifier.height((90 * scale).dp))
    }
}

@Composable
fun AchievementCard(achievement: Achievement, scale: Float, strings: Strings) {
    val isDark = isSystemInDarkTheme()
    val iconTint = when {
        achievement.isUnlocked -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(
            (2 * scale).dp,
            MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                modifier = Modifier.size((48 * scale).dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.width((16 * scale).dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    fontSize = (24 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = achievement.description,
                    fontSize = (18 * scale).sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                // ✅ CANVI: Mostrar sempre per fites de temps, o només si no està desbloquejada
                if (!achievement.isUnlocked || achievement.isTimeBasedReverse) {
                    Spacer(modifier = Modifier.height((8 * scale).dp))

                    // Progress
                    val displayProgress = if (achievement.isTimeBasedReverse) {
                        if (achievement.currentValue >= 999) 0f
                        else {
                            // ✅ Si està desbloquejada, barra completa
                            if (achievement.isUnlocked) 1f
                            else (achievement.targetValue.toFloat() - achievement.currentValue) / achievement.targetValue
                        }
                    } else {
                        achievement.progress
                    }

                    // ✅ Només mostrar barra si NO està desbloquejada
                    if (!achievement.isUnlocked) {
                        LinearProgressIndicator(
                            progress = { displayProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = (4 * scale).dp)
                        )
                    }

                    // ✅ Text sempre visible per fites de temps
                    val displayText = if (achievement.isTimeBasedReverse) {
                        val currentTime = if (achievement.currentValue >= 999) "--:--"
                        else StatisticsManager.formatTime(achievement.currentValue)
                        val targetTime = StatisticsManager.formatTime(achievement.targetValue)
                        if (achievement.isUnlocked) {
                            "✓ $currentTime / $targetTime"  // ✅ Mostrar temps aconseguit
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
                        fontSize = (16 * scale).sp,
                        color = if (achievement.isUnlocked) {
                            Color(0xFF4CAF50)  // ✅ Verd per aconseguides
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}

