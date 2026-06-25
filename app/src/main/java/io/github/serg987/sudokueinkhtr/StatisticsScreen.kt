package io.github.serg987.sudokueinkhtr

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
fun StatisticsScreen() {
    val context = LocalContext.current
    val strings = rememberStrings()
    val stats = remember { StatisticsManager.loadStatistics(context) }
    val scale = AdaptiveSizes.getScaleFactor()

    // Calcular total de partides
    val totalGames = stats.totalGamesCompleted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((16 * scale).dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height((46 * scale).dp))

        // Títol
        Text(
            text = strings.statistics,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (36 * scale).sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.Black
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
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height((24 * scale).dp))

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
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height((24 * scale).dp))

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
        StatCard(
            title = strings.totalGames,
            gamesCompleted = totalGames,
            bestTime = null,  // No mostrar temps
            strings = strings,
            scale = scale,
            isTotalCard = true  // ✅ Mode totals
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    gamesCompleted: Int,
    bestTime: Int?,
    strings: Strings,
    scale: Float,
    isAttackMode: Boolean = false,
    isTotalCard: Boolean = false  // ✅ NOU: Paràmetre per card totals
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(
            if (isTotalCard) 0.80f else 1f
            )
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            (2 * scale).dp,
            Color.Black
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding((12 * scale).dp)
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = if (isTotalCard) Alignment.CenterHorizontally else Alignment.Start  // ✅ Centrat per totals
        ) {
            // Títol
            Text(
                text = title,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height((2 * scale).dp))

            // Partides totals
            Text(
                text = if (isTotalCard) {
                    "$gamesCompleted"  // ✅ Només número per totals
                } else {
                    "${strings.gamesCompleted} $gamesCompleted"  // ✅ Label + número per cards normals
                },
                fontSize = if (isTotalCard) (32 * scale).sp else (20 * scale).sp,
                //fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (!isTotalCard) {  // ✅ Només mostrar temps si NO és card totals
                Spacer(modifier = Modifier.height((2 * scale).dp))
                val timeLabel = if (isAttackMode) strings.bestRemainingTime else strings.bestTime
                Text(
                    text = "$timeLabel ${bestTime?.let { StatisticsManager.formatTime(it) } ?: "--:--"}",
                    fontSize = (20 * scale).sp,
                    color = Color.Black
                )
            }
        }
    }
}


