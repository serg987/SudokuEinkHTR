package com.ktacrack.sudokueink

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()  // ← Afegeix això
    val stats = remember { StatisticsManager.loadStatistics(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.Start)
                .height(50.dp)
                .width(160.dp)
        ) {
            Text(
                strings.back,  // ← Canviat
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = strings.statisticsTitle,  // ← Canviat
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Estadístiques per dificultat
        StatisticCard(
            difficulty = strings.difficultyEasy,  // ← Canviat
            gamesCompleted = stats.gamesCompletedEasy,
            bestTime = stats.bestTimeEasy,
            strings = strings  // ← Passa strings
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatisticCard(
            difficulty = strings.difficultyMedium,  // ← Canviat
            gamesCompleted = stats.gamesCompletedMedium,
            bestTime = stats.bestTimeMedium,
            strings = strings  // ← Passa strings
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatisticCard(
            difficulty = strings.difficultyHard,  // ← Canviat
            gamesCompleted = stats.gamesCompletedHard,
            bestTime = stats.bestTimeHard,
            strings = strings  // ← Passa strings
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Total de partides
        val totalGames = stats.gamesCompletedEasy +
                stats.gamesCompletedMedium +
                stats.gamesCompletedHard

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(2.dp, Color.Black),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.totalGames,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 36.sp,
                        color = Color.DarkGray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$totalGames",
                    fontSize = 38.sp,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun StatisticCard(
    difficulty: String,
    gamesCompleted: Int,
    bestTime: Int?,
    strings: Strings  // ← Afegeix aquest paràmetre
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.Black),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = difficulty,
                fontSize = 28.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = strings.gamesCompleted,  // ← Canviat
                        fontSize = 22.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "$gamesCompleted",
                        fontSize = 26.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.bestTime,  // ← Canviat
                        fontSize = 22.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = bestTime?.let { formatTime(it) } ?: "--:--",
                        fontSize = 26.sp
                    )
                }
            }
        }
    }
}
