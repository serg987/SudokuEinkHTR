package com.ktacrack.sudokueink

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val targetValue: Int,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedDate: Long? = null,
    val isTimeBasedReverse: Boolean = false
) {
    val progress: Float get() = (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}

// Per desar
@Serializable
data class AchievementData(
    val id: String,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockedDate: Long?
)

