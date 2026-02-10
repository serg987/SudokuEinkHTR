package com.ktacrack.sudokueink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()
    val stats = remember { StatisticsManager.loadStatistics(context) }
    val scale = AdaptiveSizes.getScaleFactor()

    // Calcular total de partides
    val totalGames = stats.gamesCompletedEasyNormal +
            stats.gamesCompletedMediumNormal +
            stats.gamesCompletedHardNormal +
            stats.gamesCompletedEasyAttack +
            stats.gamesCompletedMediumAttack +
            stats.gamesCompletedHardAttack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((16 * scale).dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height((38 * scale).dp))

        // ====== BOTÓ TORNAR (DALT ESQUERRA) ======
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .height((50 * scale).dp)
                    .width((160 * scale).dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(strings.back, fontSize = (20 * scale).sp)
            }
        }

        Spacer(modifier = Modifier.height((16 * scale).dp))

        // Títol
        Text(
            text = strings.statistics,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (36 * scale).sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height((32 * scale).dp))

        // ====== 2 COLUMNES PARAL·LELES ======
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((16 * scale).dp)
        ) {
            // COLUMNA 1: JOC NORMAL
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.normalMode,
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height((16 * scale).dp))

                // EASY Normal
                StatCard(
                    title = strings.difficultyEasy,
                    gamesCompleted = stats.gamesCompletedEasyNormal,
                    bestTime = stats.bestTimeEasyNormal,
                    strings = strings,
                    scale = scale
                )

                Spacer(modifier = Modifier.height((12 * scale).dp))

                // MEDIUM Normal
                StatCard(
                    title = strings.difficultyMedium,
                    gamesCompleted = stats.gamesCompletedMediumNormal,
                    bestTime = stats.bestTimeMediumNormal,
                    strings = strings,
                    scale = scale
                )

                Spacer(modifier = Modifier.height((12 * scale).dp))

                // HARD Normal
                StatCard(
                    title = strings.difficultyHard,
                    gamesCompleted = stats.gamesCompletedHardNormal,
                    bestTime = stats.bestTimeHardNormal,
                    strings = strings,
                    scale = scale
                )
            }

            // COLUMNA 2: SUDOKU ATAC
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.attackMode,
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height((16 * scale).dp))

                // EASY Attack
                StatCard(
                    title = strings.difficultyEasy,
                    gamesCompleted = stats.gamesCompletedEasyAttack,
                    bestTime = stats.bestTimeEasyAttack,
                    strings = strings,
                    scale = scale,
                    isAttackMode = true // ✅ AFEGIR
                )

                Spacer(modifier = Modifier.height((12 * scale).dp))

                // MEDIUM Attack
                StatCard(
                    title = strings.difficultyMedium,
                    gamesCompleted = stats.gamesCompletedMediumAttack,
                    bestTime = stats.bestTimeMediumAttack,
                    strings = strings,
                    scale = scale,
                    isAttackMode = true // ✅ AFEGIR
                )

                Spacer(modifier = Modifier.height((12 * scale).dp))

                // HARD Attack
                StatCard(
                    title = strings.difficultyHard,
                    gamesCompleted = stats.gamesCompletedHardAttack,
                    bestTime = stats.bestTimeHardAttack,
                    strings = strings,
                    scale = scale,
                    isAttackMode = true // ✅ AFEGIR
                )
            }
        }

        Spacer(modifier = Modifier.height((32 * scale).dp))

        // ====== TOTAL DE PARTIDES ======
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height((90 * scale).dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((12 * scale).dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.totalGames,
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height((4 * scale).dp))
                Text(
                    text = "$totalGames",
                    fontSize = (32 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    gamesCompleted: Int,
    bestTime: Int?,
    strings: Strings,
    scale: Float,
    isAttackMode: Boolean = false // ✅ NOU paràmetre
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height((110 * scale).dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke((2 * scale).dp, Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding((12 * scale).dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // Títol (Fàcil, Intermedi, Difícil)
            Text(
                text = title,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height((8 * scale).dp))

            // Partides completades amb label
            Text(
                text = "${strings.gamesCompleted} $gamesCompleted",
                fontSize = (20 * scale).sp,
                color = Color.DarkGray
            )

            // ✅ Canviar el label segons el mode
            val timeLabel = if (isAttackMode) strings.bestRemainingTime else strings.bestTime

            Text(
                text = "$timeLabel ${bestTime?.let { StatisticsManager.formatTime(it) } ?: "--:--"}",
                fontSize = (20 * scale).sp,
                color = Color.DarkGray
            )
        }
    }
}

