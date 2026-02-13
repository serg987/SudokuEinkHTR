package com.ktacrack.sudokueink

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.combinedClickable


enum class NotesMode {
    OFF, MANUAL, AUTO
}

@Composable
fun GameScreen(
    difficulty: Difficulty,
    mode: GameMode,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = rememberStrings()

    var resetTrigger by remember { mutableStateOf(0) }
    var resetTimerTrigger by remember(resetTrigger) { mutableStateOf(0) } // Reiniciar timer

    // Intentar carregar partida guardada
    val savedGame = remember(resetTrigger) {
        if (resetTrigger == 0) {
            GameStateManager.loadGame(context, mode, difficulty)  // ← Passa tipus joc i difficulty
        } else null
    }

    val initialGame = remember(resetTrigger) {
        if (savedGame != null) {
            // Restaurar des de partida guardada
            val restoredBoard = savedGame.board.map { row ->
                row.map { savedCell ->
                    SudokuCell(
                        value = savedCell.value,
                        isFixed = savedCell.isFixed,
                        notes = savedCell.notes.toSet()
                    )
                }
            }
            SudokuGame(restoredBoard, savedGame.solution)
        } else {
            // Generar nou joc
            SudokuGenerator.generate(difficulty)
        }
    }

    var isPaused by remember(resetTrigger) { mutableStateOf(savedGame != null) } // Començar pausat si hi ha partida guardada
    var showPauseDialog by remember { mutableStateOf(false) }
    var shouldSaveOnExit by remember(resetTrigger) { mutableStateOf(true) }

    // Factor d'escala adaptatiu
    val scale = AdaptiveSizes.getScaleFactor()

    // Detectar orientació
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Guardar el tauler REALMENT inicial (només números fixos) per poder reiniciar
    val initialBoard = remember(resetTrigger) {
        if (savedGame != null) {
            // Si ve de partida guardada, crear tauler només amb fixos
            savedGame.board.map { row ->
                row.map { savedCell ->
                    SudokuCell(
                        value = if (savedCell.isFixed) savedCell.value else 0,
                        isFixed = savedCell.isFixed,
                        notes = emptySet()
                    )
                }
            }
        } else {
            // Si és nou joc, usar l'inicial directament
            initialGame.board.map { row ->
                row.map { cell -> cell.copy() }
            }
        }
    }

    // Guardem la solució correcta
    val solution = remember(resetTrigger) { initialGame.solution }

    // Estat editable del tauler
    var boardState by remember(resetTrigger) {
        mutableStateOf(
            initialGame.board.map { row ->
                row.map { cell -> cell.copy() }
            }
        )
    }



    var selectedCell by remember(resetTrigger) { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedNumber by remember(resetTrigger) { mutableStateOf<Int?>(null) }
    var showVictoryDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showErrorDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showTimeoutDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showResumeDialog by remember(resetTrigger) { mutableStateOf(savedGame != null) }
    var showDrawingCanvas by remember { mutableStateOf(false) }
    var isPencilMode by remember(resetTrigger) { mutableStateOf(false) }

    // Achievements
    var showAchievementUnlocked by remember { mutableStateOf(false) }
    var unlockedAchievement by remember { mutableStateOf<Achievement?>(null) }
    var pendingVictoryDialog by remember { mutableStateOf(false) }
    var pendingClearGame by remember { mutableStateOf(false) }
    var unlockedQueue by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var showingAchievement by remember { mutableStateOf<Achievement?>(null) }

    // Temps inicial (restaurar si hi ha partida guardada)
    var startTimeOffset by remember(resetTrigger) { mutableStateOf(savedGame?.elapsedSeconds ?: 0) }

    // Variable per guardar els segons actuals
    var currentElapsedSeconds by remember(resetTrigger) { mutableStateOf(startTimeOffset) }

    // Variable per guardar el temps quan es fa pauta/finestra error
    var pausedAtSeconds by remember(resetTrigger) { mutableStateOf(0) }

    // Time Attack: límit de temps segons mode i dificultat
    val timeLimitSeconds = remember(mode, difficulty) {
        if (mode == GameMode.ATTACK) {
            when (difficulty) {
                Difficulty.EASY -> 20 * 60
                Difficulty.MEDIUM -> 30 * 60
                Difficulty.HARD -> 45 * 60
            }
        } else {
            null
        }
    }

    // Temps acumulat
    val timerText = rememberTimer(
        resetTrigger = resetTrigger,
        startOffset = startTimeOffset,
        isPaused = isPaused,
        resetTimer = resetTimerTrigger,
        onTimeUpdate = { elapsedSeconds ->
            currentElapsedSeconds = elapsedSeconds
        }
    )

    // Temps restant
    val displayTimerText = if (timeLimitSeconds != null) {
        val remaining = (timeLimitSeconds - currentElapsedSeconds).coerceAtLeast(0)
        val minutes = remaining / 60
        val seconds = remaining % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    } else {
        timerText
    }


    var hintsRemaining by rememberSaveable {
        mutableIntStateOf(
            savedGame?.hintsRemaining ?: when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 1
            }
        )
    }

    // Reseteja pistes quan es genera nou joc
    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {  // Només si és nou joc (no inicial)
            hintsRemaining = when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 1
            }
        }
    }

    var notesMode by remember(resetTrigger) { mutableStateOf(NotesMode.OFF) }
    var showAutoNotesDialog by remember { mutableStateOf(false) }
    val moveHistory = rememberSaveable { mutableStateListOf<List<List<SudokuCell>>>() }

    // Guardar automàticament cada canvi
    LaunchedEffect(boardState, hintsRemaining) {
        if (!showVictoryDialog) {
            val savedState = SavedGameState(
                difficulty = difficulty.name,
                mode = mode.name,
                board = boardState.map { row ->
                    row.map { cell ->
                        SavedCell(
                            value = cell.value,
                            isFixed = cell.isFixed,
                            notes = cell.notes.toList()
                        )
                    }
                },
                solution = solution,
                elapsedSeconds = currentElapsedSeconds,  // ← Usar la variable
                hintsRemaining = hintsRemaining
            )
            GameStateManager.saveGame(context, savedState)
        }
    }

    // Guardar quan l'usuari surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            if (!showVictoryDialog && shouldSaveOnExit) {
                val savedState = SavedGameState(
                    difficulty = difficulty.name,
                    mode = mode.name,
                    board = boardState.map { row ->
                        row.map { cell ->
                            SavedCell(
                                value = cell.value,
                                isFixed = cell.isFixed,
                                notes = cell.notes.toList()
                            )
                        }
                    },
                    solution = solution,
                    elapsedSeconds = currentElapsedSeconds,  // ← Usar la variable
                    hintsRemaining = hintsRemaining
                )
                GameStateManager.saveGame(context, savedState)
            }
        }
    }

    //Neteja la partica
    LaunchedEffect(pendingClearGame) {
        if (pendingClearGame) {
            GameStateManager.clearGame(context, mode, difficulty)
            pendingClearGame = false
        }
    }

    // Funció omplir notes automàtiques
    fun getValidNotesForCell(row: Int, col: Int): Set<Int> {
        val usedNumbers = mutableSetOf<Int>()

        // Números a la mateixa fila
        for (c in 0 until 9) {
            if (boardState[row][c].value != 0) {
                usedNumbers.add(boardState[row][c].value)
            }
        }

        // Números a la mateixa columna
        for (r in 0 until 9) {
            if (boardState[r][col].value != 0) {
                usedNumbers.add(boardState[r][col].value)
            }
        }

        // Números al mateix bloc 3x3
        val blockRow = (row / 3) * 3
        val blockCol = (col / 3) * 3
        for (r in blockRow until blockRow + 3) {
            for (c in blockCol until blockCol + 3) {
                if (boardState[r][c].value != 0) {
                    usedNumbers.add(boardState[r][c].value)
                }
            }
        }

        // Retornar números vàlids (1-9 menys els usats)
        return (1..9).toSet() - usedNumbers
    }

    // Netejar notes automàtiques - VERSIÓ CORREGIDA
    fun cleanAutoNotes(newBoard: List<List<SudokuCell>>): List<List<SudokuCell>> {
        if (notesMode != NotesMode.AUTO) return newBoard

        // ✅ Recalcular notes per TOTES les cel·les del tauler
        return newBoard.mapIndexed { row, rowList ->
            rowList.mapIndexed { col, cell ->
                if (cell.isFixed || cell.value != 0) {
                    // Cel·la amb número fix o ja omplerta: no tocar
                    cell
                } else if (cell.notes.isEmpty()) {
                    // Cel·la sense notes: no tocar
                    cell
                } else {
                    // ✅ Recalcular notes vàlides basant-se en newBoard (no boardState!)
                    val usedNumbers = mutableSetOf<Int>()

                    // Números a la mateixa fila
                    for (c in 0 until 9) {
                        if (newBoard[row][c].value != 0) {
                            usedNumbers.add(newBoard[row][c].value)
                        }
                    }

                    // Números a la mateixa columna
                    for (r in 0 until 9) {
                        if (newBoard[r][col].value != 0) {
                            usedNumbers.add(newBoard[r][col].value)
                        }
                    }

                    // Números al mateix bloc 3x3
                    val blockRow = (row / 3) * 3
                    val blockCol = (col / 3) * 3
                    for (r in blockRow until blockRow + 3) {
                        for (c in blockCol until blockCol + 3) {
                            if (newBoard[r][c].value != 0) {
                                usedNumbers.add(newBoard[r][c].value)
                            }
                        }
                    }

                    // Retornar només notes vàlides
                    val validNotes = (1..9).toSet() - usedNumbers
                    cell.copy(notes = cell.notes.intersect(validNotes))
                }
            }
        }
    }

    // Diàleg AUTO
    if (showAutoNotesDialog) {
        AlertDialog(
            onDismissRequest = { showAutoNotesDialog = false },
            title = {
                Text(
                    text = strings.autoNotesTitle,  // "Notes automàtiques"
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.autoNotesExplanation,  // Explicació
                    fontSize = (20 * scale).sp,
                    lineHeight = (28 * scale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAutoNotesDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp)
                ) {
                    Text(strings.understood, fontSize = (20 * scale).sp)  // "Entès"
                }
            }
        )
    }

    // Diàleg fites + victòria
    LaunchedEffect(pendingVictoryDialog) {
        if (pendingVictoryDialog) {
            // 1️⃣ Comprovar achievements
            val updatedStats = StatisticsManager.loadStatistics(context)
            val newlyUnlocked = mutableListOf<Achievement>()
            var unlockedSomething = false

            AchievementManager.checkAchievements(
                context = context,
                stats = updatedStats,
                strings = strings,
                onNewAchievement = { newAchievement ->
                    newlyUnlocked.add(newAchievement)
                }
            )
            // Van sortint les fites
            if (newlyUnlocked.isNotEmpty()) {
                unlockedQueue = newlyUnlocked
                showingAchievement = unlockedQueue.first()
            } else { // Quan s'acaben o no hi ha fites, diàleg felicitats
                showVictoryDialog = true
                pendingClearGame = true
                pendingVictoryDialog = false
            }
        }
    }

    // Diàleg fites
    if (showingAchievement != null) {
        AlertDialog(
            onDismissRequest = { /* forcem a clicar botó */ },
            icon = {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    modifier = Modifier.size((64 * scale).dp),
                    tint = Color(0xFFFFD700)
                )
            },
            title = { Text("🏆 ${strings.achievementUnlocked}") },
            text = {
                Column {
                    Text(
                        showingAchievement!!.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = (20 * scale).sp
                    )
                    Text(
                        showingAchievement!!.description,
                        fontSize = (16 * scale).sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Treure el primer de la cua
                    unlockedQueue = unlockedQueue.drop(1)

                    if (unlockedQueue.isNotEmpty()) {
                        showingAchievement = unlockedQueue.first()
                    } else {
                        // Ja no queden fites → ara felicitats
                        showingAchievement = null
                        showVictoryDialog = true
                        pendingClearGame = true
                        pendingVictoryDialog = false
                    }
                }) {
                    Text(strings.awesome)
                }
            }
        )
    }


    // Funció per actualitzar el tauler i guardar a l'historial
    fun updateBoard(newBoard: List<List<SudokuCell>>) {
        moveHistory.add(boardState)
        val cleanedBoard = cleanAutoNotes(newBoard)
        boardState = cleanedBoard
    }

    // Comprovar si el tauler està complet i correcte
    val isComplete = boardState.all { row -> row.all { it.value != 0 } }
    val isCorrect = if (isComplete) {
        boardState.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                cell.value == solution[r][c]
            }.all { it }
        }.all { it }
    } else {
        false
    }

    // Time Attack: comprovar fi de temps
    LaunchedEffect(timeLimitSeconds, currentElapsedSeconds, showVictoryDialog) {
        if (timeLimitSeconds != null &&
            !showVictoryDialog &&
            currentElapsedSeconds >= timeLimitSeconds
        ) {
            // Aquí podries mostrar un diàleg de derrota per temps, per ara només:
            isPaused = true
            showTimeoutDialog = true  // o un altre estat específic si vols un diàleg diferent
            shouldSaveOnExit = false
        }
    }

    // Detectar quan es completa el Sudoku
    LaunchedEffect(boardState) {
        val complete = boardState.all { row -> row.all { it.value != 0 } }
        if (complete && !showVictoryDialog) {
            val correct = boardState.mapIndexed { r, row ->
                row.mapIndexed { c, cell -> cell.value == solution[r][c] }
            }.all { it.all { it } }

            if (correct) {
                // ✅ Aturar quan es completa correctament
                isPaused = true
                StatisticsManager.recordCompletion(context, difficulty, mode, currentElapsedSeconds)
                pendingVictoryDialog = true   // ⬅️ el mostrem després de possibles achievements
            } else {
                // ✅ Només mostrar el diàleg (el LaunchedEffect dins del diàleg pausarà el timer)
                showErrorDialog = true
            }
        }
    }

    if (isLandscape) {
        // LAYOUT HORITZONTAL
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding((16 * scale).dp)
        ) {
            // COLUMNA ESQUERRA: Botó Back + Sudoku
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height((46 * scale).dp))

                // Botó Back a dalt a l'esquerra
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .offset(x = (10 * scale).dp)
                        .width((160 * scale).dp)
                        .height((50 * scale).dp)
                ) {
                    Text(strings.back, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height(0.dp))

                // Nivell i Timer SOBRE el Sudoku
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        modifier = Modifier.padding(bottom = 0.dp),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = (24 * scale).sp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPaused) {
                                    isPaused = false  // Reprendre
                                } else {
                                    isPaused = true
                                    showPauseDialog = true  // ✅ Mostrar diàleg quan pauses
                                }
                            },
                            modifier = Modifier.size((36 * scale).dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reprendre" else "Pausar",
                                modifier = Modifier
                                    .size((24 * scale).dp)
                                    .clickable {
                                        if (isPaused) {
                                            isPaused = false
                                        } else {
                                            isPaused = true
                                            showPauseDialog = true  // ✅ Mostrar diàleg
                                        }
                                    }
                            )
                        }

                        Text(text = displayTimerText, fontSize = (24 * scale).sp)
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                // Sudoku centrat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SudokuBoard(
                        board = boardState,
                        solution = solution,
                        selectedCell = selectedCell,
                        onCellClick = { row, col ->
                            if (!isPaused && !boardState[row][col].isFixed) {
                                if (isPencilMode) {
                                    selectedCell = Pair(row, col)
                                    showDrawingCanvas = true
                                } else {
                                    selectedCell = Pair(row, col)
                                }
                            }
                        },
                        onCellLongClick = { row, col ->  // ✅ AFEGIR
                            if (!isPaused && notesMode == NotesMode.AUTO && !boardState[row][col].isFixed) {
                                val validNotes = getValidNotesForCell(row, col)
                                val newBoard = boardState.mapIndexed { r, rowList ->
                                    rowList.mapIndexed { c, cell ->
                                        if (r == row && c == col) {
                                            cell.copy(notes = validNotes, value = 0)
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width((16 * scale).dp))

            // COLUMNA DRETA: Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height((36 * scale).dp))

                // Nou Joc
                Button(
                    onClick = {
                        shouldSaveOnExit = false
                        GameStateManager.clearGame(context, mode, difficulty)  // ← Passa difficulty
                        resetTrigger++
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.newGame, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((6 * scale).dp))

                // BOTÓ REINICIAR
                Button(
                    onClick = {
                        if (!isPaused) {
                            moveHistory.clear()
                            boardState = initialBoard.map { row -> row.map { cell -> cell.copy() } }
                            selectedCell = null
                            selectedNumber = null
                            startTimeOffset = 0
                            hintsRemaining = when (difficulty) {
                                Difficulty.EASY -> 5
                                Difficulty.MEDIUM -> 3
                                Difficulty.HARD -> 1
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.reset, fontSize = (20 * scale).sp)
                }


                Spacer(modifier = Modifier.height((20 * scale).dp))

                // Números grid 3x3
                Column {
                    for (rowNum in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (colNum in 0 until 3) {
                                val number = rowNum * 3 + colNum + 1
                                Button(
                                    onClick = {  // ✅ AQUÍ! Substituir tot el onClick
                                        if (!isPaused) {
                                            selectedCell?.let { (row, col) ->
                                                if (!boardState[row][col].isFixed) {
                                                    selectedNumber = number
                                                    val newBoard = boardState.mapIndexed { r, rowList ->
                                                        rowList.mapIndexed { c, cell ->
                                                            if (r == row && c == col) {
                                                                when (notesMode) {  // ✅ Canviar de isNotesMode a notesMode
                                                                    NotesMode.MANUAL -> {
                                                                        val newNotes = if (cell.notes.contains(number)) {
                                                                            cell.notes - number
                                                                        } else {
                                                                            cell.notes + number
                                                                        }
                                                                        cell.copy(notes = newNotes)
                                                                    }
                                                                    NotesMode.AUTO -> {
                                                                        // En mode AUTO, posar número directament
                                                                        cell.copy(value = number, notes = emptySet())
                                                                    }
                                                                    NotesMode.OFF -> {
                                                                        cell.copy(value = number, notes = emptySet())
                                                                    }
                                                                }
                                                            } else {
                                                                cell
                                                            }
                                                        }
                                                    }
                                                    updateBoard(newBoard)
                                                    selectedNumber = null
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size((50 * scale).dp)
                                        .padding((2 * scale).dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedNumber == number) Color.Black else Color.White,
                                        contentColor = if (selectedNumber == number) Color.White else Color.Black
                                    ),
                                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = number.toString(), fontSize = (32 * scale).sp)
                                }
                            }
                        }
                        // Afegir espai entre files (excepte després de l'última)
                        if (rowNum < 2) {
                            Spacer(modifier = Modifier.height((8 * scale).dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height((24 * scale).dp))

                // 5 botons en columna
                // BOTÓ LLAPIS MODE
                Button(
                    onClick = {
                        if (!isPaused) {
                            isPencilMode = !isPencilMode
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isPencilMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        if (isPencilMode) strings.pencilOn else strings.pencilOff,
                        fontSize = (20 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                // BOTÓ NOTES amb 3 estats
                Button(
                    onClick = {
                        if (!isPaused) {
                            notesMode = when (notesMode) {
                                NotesMode.OFF -> NotesMode.MANUAL
                                NotesMode.MANUAL -> {
                                    showAutoNotesDialog = true  // Mostrar explicació
                                    NotesMode.AUTO
                                }
                                NotesMode.AUTO -> NotesMode.OFF
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = when (notesMode) {
                            NotesMode.OFF -> strings.notesOff
                            NotesMode.MANUAL -> strings.notesManual  // ✅ AFEGIR a Strings
                            NotesMode.AUTO -> strings.notesAuto  // ✅ AFEGIR a Strings
                        },
                        fontSize = (20 * scale).sp
                    )
                }


                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused) {
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed) {
                                    val newBoard = boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                cell.copy(value = 0, notes = emptySet())
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                    updateBoard(newBoard)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.erase, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused && hintsRemaining > 0) {
                            val emptyCells = mutableListOf<Pair<Int, Int>>()
                            for (r in 0 until 9) {
                                for (c in 0 until 9) {
                                    if (boardState[r][c].value == 0 && !boardState[r][c].isFixed) {
                                        emptyCells.add(Pair(r, c))
                                    }
                                }
                            }
                            if (emptyCells.isNotEmpty()) {
                                val (r, c) = emptyCells.random()
                                val newBoard = boardState.mapIndexed { row, rowList ->
                                    rowList.mapIndexed { col, cell ->
                                        if (row == r && col == c) {
                                            cell.copy(value = solution[r][c], notes = emptySet())
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
                                hintsRemaining--
                            }
                        }
                    },
                    enabled = hintsRemaining > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("${strings.hint} ($hintsRemaining)", fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused && moveHistory.isNotEmpty()) {
                            boardState = moveHistory.removeAt(moveHistory.lastIndex)
                        }
                    },
                    enabled = moveHistory.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.undo, fontSize = (20 * scale).sp)
                }
            }
        }
    } else {
        // LAYOUT VERTICAL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding((16 * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height((46 * scale).dp))

            // FILA SUPERIOR: Botó tornar (esquerra) i Nou Joc (dreta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((160 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        strings.back,
                        fontSize = (24 * scale).sp
                    )
                }

                Button(
                    onClick = {
                        shouldSaveOnExit = false
                        GameStateManager.clearGame(context, mode, difficulty)  // ← Passa difficulty
                        resetTrigger++
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((180 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        strings.newGame,
                        fontSize = (20 * scale).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height((8 * scale).dp))

            // FILA AMB NIVELL/TIMER CENTRAT I BOTÓ REINICIAR A LA DRETA
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Spacer(modifier = Modifier.width((180 * scale).dp)) // Espai per equilibrar

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        modifier = Modifier.padding(bottom = 0.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (24 * scale).sp
                        )
                    )

                    Spacer(modifier = Modifier.height((0 * scale).dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Reprendre" else "Pausar",
                            modifier = Modifier
                                .size((24 * scale).dp)
                                .clickable {
                                    if (isPaused) {
                                        isPaused = false
                                    } else {
                                        isPaused = true
                                        showPauseDialog = true  // ✅ Mostrar diàleg
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.width((8 * scale).dp))

                        Text(text = displayTimerText, fontSize = (24 * scale).sp)
                    }
                }

                Button(
                    onClick = {
                        if (!isPaused) {
                            moveHistory.clear()
                            boardState = initialBoard.map { row -> row.map { cell -> cell.copy() } }
                            selectedCell = null
                            selectedNumber = null
                            startTimeOffset = 0
                            hintsRemaining = when (difficulty) {
                                Difficulty.EASY -> 5
                                Difficulty.MEDIUM -> 3
                                Difficulty.HARD -> 1
                            }
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((180 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(strings.reset,
                        fontSize = (20 * scale).sp)
                }
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            SudokuBoard(
                board = boardState,
                solution = solution,
                selectedCell = selectedCell,
                onCellClick = { row, col ->
                    if (!isPaused && !boardState[row][col].isFixed) {
                        if (isPencilMode) {
                            selectedCell = Pair(row, col)
                            showDrawingCanvas = true
                        } else {
                            selectedCell = Pair(row, col)
                        }
                    }
                },
                onCellLongClick = { row, col ->  // ✅ AFEGIR
                    if (!isPaused && notesMode == NotesMode.AUTO && !boardState[row][col].isFixed) {
                        val validNotes = getValidNotesForCell(row, col)
                        val newBoard = boardState.mapIndexed { r, rowList ->
                            rowList.mapIndexed { c, cell ->
                                if (r == row && c == col) {
                                    cell.copy(notes = validNotes, value = 0)
                                } else {
                                    cell
                                }
                            }
                        }
                        updateBoard(newBoard)
                    }
                }
            )

            Spacer(modifier = Modifier.height((10 * scale).dp))

            // Només els números sense botó de notes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (3 * scale).dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (number in 1..9) {
                    Button(
                        onClick = {  // ✅ AQUÍ! Substituir tot el onClick
                            if (!isPaused) {
                                selectedCell?.let { (row, col) ->
                                    if (!boardState[row][col].isFixed) {
                                        selectedNumber = number
                                        val newBoard = boardState.mapIndexed { r, rowList ->
                                            rowList.mapIndexed { c, cell ->
                                                if (r == row && c == col) {
                                                    when (notesMode) {  // ✅ Canviar de isNotesMode a notesMode
                                                        NotesMode.MANUAL -> {
                                                            val newNotes = if (cell.notes.contains(number)) {
                                                                cell.notes - number
                                                            } else {
                                                                cell.notes + number
                                                            }
                                                            cell.copy(notes = newNotes)
                                                        }
                                                        NotesMode.AUTO -> {
                                                            // En mode AUTO, posar número directament
                                                            cell.copy(value = number, notes = emptySet())
                                                        }
                                                        NotesMode.OFF -> {
                                                            cell.copy(value = number, notes = emptySet())
                                                        }
                                                    }
                                                } else {
                                                    cell
                                                }
                                            }
                                        }
                                        updateBoard(newBoard)
                                        selectedNumber = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size((52 * scale).dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedNumber == number) Color.Black else Color.White,
                            contentColor = if (selectedNumber == number) Color.White else Color.Black
                        ),
                        border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = number.toString(), fontSize = (40 * scale).sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            // FILA DE 4 BOTONS: Notes, Esborrar, Pista, Desfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // BOTÓ LLAPIS (NOU)
                Button(
                    onClick = {
                        if (!isPaused) {
                            isPencilMode = !isPencilMode
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isPencilMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        if (isPencilMode) strings.pencilOn else strings.pencilOff,
                        fontSize = (20 * scale).sp
                    )
                }

                // BOTÓ NOTES amb 3 estats
                Button(
                    onClick = {
                        if (!isPaused) {
                            notesMode = when (notesMode) {
                                NotesMode.OFF -> NotesMode.MANUAL
                                NotesMode.MANUAL -> {
                                    showAutoNotesDialog = true  // Mostrar explicació
                                    NotesMode.AUTO
                                }
                                NotesMode.AUTO -> NotesMode.OFF
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height((50 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = when (notesMode) {
                            NotesMode.OFF -> strings.notesOff
                            NotesMode.MANUAL -> strings.notesManual  // ✅ AFEGIR a Strings
                            NotesMode.AUTO -> strings.notesAuto  // ✅ AFEGIR a Strings
                        },
                        fontSize = (20 * scale).sp
                    )
                }

                // BOTÓ ESBORRAR
                Button(
                    onClick = {
                        if (!isPaused) {
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed) {
                                    val newBoard = boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                cell.copy(value = 0, notes = emptySet())
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                    updateBoard(newBoard)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.erase, fontSize = (20 * scale).sp)
                }

                // BOTÓ PISTA
                Button(
                    onClick = {
                        if (!isPaused && hintsRemaining > 0) {
                            val emptyCells = mutableListOf<Pair<Int, Int>>()
                            for (r in 0 until 9) {
                                for (c in 0 until 9) {
                                    if (boardState[r][c].value == 0 && !boardState[r][c].isFixed) {
                                        emptyCells.add(Pair(r, c))
                                    }
                                }
                            }
                            if (emptyCells.isNotEmpty()) {
                                val (r, c) = emptyCells.random()
                                val newBoard = boardState.mapIndexed { row, rowList ->
                                    rowList.mapIndexed { col, cell ->
                                        if (row == r && col == c) {
                                            cell.copy(value = solution[r][c], notes = emptySet())
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
                                hintsRemaining--
                            }
                        }
                    },
                    enabled = hintsRemaining > 0,
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("${strings.hint} ($hintsRemaining)", fontSize = (20 * scale).sp)
                }

                // BOTÓ DESFER
                Button(
                    onClick = {
                        if (!isPaused && moveHistory.isNotEmpty()) {
                            boardState = moveHistory.removeAt(moveHistory.lastIndex)
                        }
                    },
                    enabled = moveHistory.isNotEmpty(),
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.undo, fontSize = (20 * scale).sp)
                }
            }
        }
    }

    // Diàleg de canvas de dibuix
    if (showDrawingCanvas) {
        AlertDialog(
            onDismissRequest = { showDrawingCanvas = false },
            title = { Text("Reconeixement de dígits", fontSize = (24 * scale).sp) },
            text = {
                DrawingCanvas(
                    onDigitRecognized = { digit ->
                        // Col·locar el dígit reconegut a la cella seleccionada
                        selectedCell?.let { (row, col) ->
                            if (!boardState[row][col].isFixed && digit in 1..9) {
                                val newBoard = boardState.mapIndexed { r, rowList ->
                                    rowList.mapIndexed { c, cell ->
                                        if (r == row && c == col) {
                                            // ✅ CANVIAR AQUESTA PART:
                                            when (notesMode) {  // ✅ Canviar de isNotesMode a notesMode
                                                NotesMode.MANUAL -> {
                                                    // Mode notes: afegir/treure nota
                                                    val newNotes = if (cell.notes.contains(digit)) {
                                                        cell.notes - digit
                                                    } else {
                                                        cell.notes + digit
                                                    }
                                                    cell.copy(notes = newNotes)
                                                }
                                                NotesMode.AUTO, NotesMode.OFF -> {
                                                    // Mode normal: posar número
                                                    cell.copy(value = digit, notes = emptySet())
                                                }
                                            }
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
                                showDrawingCanvas = false
                            }
                        }
                    },
                    onDismiss = { showDrawingCanvas = false }
                )
            },
            confirmButton = { }
        )
    }

    // Diàleg de Pause
    if (showPauseDialog && isPaused) {
        AlertDialog(
            onDismissRequest = { /* No permetre tancar sense reprendre */ },
            title = {
                Text(
                    text = strings.gamePaused,  // ✅ Afegir a Strings.kt
                    fontSize = (32 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = strings.gamePausedMessage,  // ✅ "Prem reprendre per continuar jugant"
                    fontSize = (24 * scale).sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPaused = false
                        showPauseDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp)
                ) {
                    Text(strings.resume, fontSize = (20 * scale).sp)  // ✅ "Reprendre"
                }
            },
            dismissButton = null
        )
    }

    // Diàleg de victòria
    if (showVictoryDialog) {
        AlertDialog(
            onDismissRequest = { showVictoryDialog = false },
            title = {
                Text(
                    text = strings.congratulations,
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp
                )
            },
            text = {
                Text(
                    text = "${strings.completed}\n\n${strings.time} $displayTimerText",
                    fontSize = (24 * scale).sp,
                    lineHeight = (32 * scale).sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    // Botó NOU SUDOKU
                    Button(
                        onClick = {
                            showVictoryDialog = false
                            shouldSaveOnExit = false
                            GameStateManager.clearGame(context, mode, difficulty)
                            resetTrigger++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.newGame, fontSize = (18 * scale).sp)
                    }

                    // Botó TORNAR AL MENÚ
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.backToMenu, fontSize = (18 * scale).sp)
                    }
                }
            },
            dismissButton = null
        )
    }

    // Diàleg d'error
    if (showErrorDialog) {
        // ✅ Guardar el temps actual i pausar
        LaunchedEffect(Unit) {
            pausedAtSeconds = currentElapsedSeconds
            isPaused = true
        }

        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                // ✅ Actualitzar l'offset al temps pausat abans de reprendre
                startTimeOffset = pausedAtSeconds
                isPaused = false
            },
            title = {
                Text(
                    text = strings.error,
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = strings.errorMessage,
                    fontSize = (24 * scale).sp,
                    lineHeight = (32 * scale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                        // ✅ Actualitzar l'offset al temps pausat abans de reprendre
                        startTimeOffset = pausedAtSeconds
                        isPaused = false
                    },
                    modifier = Modifier.height((50 * scale).dp)
                ) {
                    Text(strings.review, fontSize = (18 * scale).sp)
                }
            }
        )
    }

    // Diàleg de timeout (Time Attack)
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showTimeoutDialog = false
                // Opcionalment pots reprendre el timer si vols
                // isPaused = false
            },
            title = {
                Text(
                    text = "⏱️ ${strings.timeUp}", // Afegeix "timeUp" a Strings.kt
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = strings.timeUpMessage, // Afegeix missatge a Strings.kt
                    fontSize = (24 * scale).sp,
                    lineHeight = (32 * scale).sp
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    // Botó NOU SUDOKU
                    Button(
                        onClick = {
                            showTimeoutDialog = false
                            shouldSaveOnExit = false
                            GameStateManager.clearGame(context, mode, difficulty)
                            resetTrigger++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.newGame, fontSize = (18 * scale).sp)
                    }

                    // Botó TORNAR AL MENÚ
                    Button(
                        onClick = { onBack() },
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.backToMenu, fontSize = (18 * scale).sp)
                    }
                }
            },
            dismissButton = null
        )
    }
    // Diàleg de continuar partida guardada
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = {
                // No permetre tancar sense decidir
            },
            title = {
                Text(
                    text = "${strings.resumeGame}", // Afegeix a Strings.kt
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp
                )
            },
            text = {
                Column {
                    Text(
                        text = strings.resumeGameMessage, // Afegeix a Strings.kt
                        fontSize = (24 * scale).sp,
                        lineHeight = (32 * scale).sp
                    )
                    Spacer(modifier = Modifier.height((16 * scale).dp))
                    Text(
                        text = "${strings.time}: $displayTimerText",
                        fontSize = (20 * scale).sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    // Botó CONTINUAR
                    Button(
                        onClick = {
                            showResumeDialog = false
                            isPaused = false // ✅ Començar a comptar
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.continue_, fontSize = (18 * scale).sp) // Afegeix a Strings.kt
                    }

                    // Botó NOU JOC
                    Button(
                        onClick = {
                            showResumeDialog = false
                            shouldSaveOnExit = false
                            GameStateManager.clearGame(context, mode, difficulty)
                            resetTrigger++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(strings.newGame, fontSize = (18 * scale).sp)
                    }
                }
            },
            dismissButton = null
        )
    }
}
@Composable
fun SudokuBoard(
    board: List<List<SudokuCell>>,
    solution: List<List<Int>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    onCellLongClick: ((Int, Int) -> Unit)? = null  // ✅ AFEGIR
) {
    val scale = AdaptiveSizes.getScaleFactor()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // Calcular mida real de cada cel·la
    val cellSize = (screenWidthDp - 32) / 9  // Restar padding i dividir per 9
    val noteScale = (cellSize / 40f).coerceIn(0.2f, 2.2f)  // Escala específica per notes

    // Padding adaptatiu
    val notePadding = when {
        screenWidthDp < 360 -> (2 * scale).dp
        screenWidthDp < 600 -> (2 * scale).dp
        else -> (8 * scale).dp
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border((3 * scale).dp, Color.Black)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 9) {
                    val cell = board[row][col]
                    val isSelected = selectedCell == Pair(row, col)

                    // Determinar el color del text
                    val textColor = when {
                        cell.isFixed -> Color(0xFF263238) // Números inicials
                        cell.value == 0 -> Color.Black  // Cel·la buida
                        else -> Color(0xFF2196F3)  // Tots els números de l'usuari en blau
                    }

                    val topBorder = when {
                        row == 3 || row == 6 -> (2 * scale).dp
                        else -> (0.5 * scale).dp
                    }
                    val leftBorder = when {
                        col == 3 || col == 6 -> (2 * scale).dp
                        else -> (0.5 * scale).dp
                    }
                    val bottomBorder = if (row == 8) 0.dp else (0.5 * scale).dp
                    val rightBorder = if (col == 8) 0.dp else (0.5 * scale).dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    isSelected -> Color(0xFFFFE082)     // Seleccionada: groc pastel
                                    cell.isFixed -> Color(0xFFE0E0E0)   // Fixa: gris mitjà
                                    else -> Color.White                 // Editable: blanc
                                }
                            )
                            .combinedClickable(
                                onClick = { onCellClick(row, col) },
                                onLongClick = { onCellLongClick?.invoke(row, col) }
                            )
                            .drawBehind {
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(0f, topBorder.toPx() / 2),
                                    end = Offset(size.width, topBorder.toPx() / 2),
                                    strokeWidth = topBorder.toPx()
                                )
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(leftBorder.toPx() / 2, 0f),
                                    end = Offset(leftBorder.toPx() / 2, size.height),
                                    strokeWidth = leftBorder.toPx()
                                )
                                if (bottomBorder > 0.dp) {
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(
                                            0f,
                                            size.height - bottomBorder.toPx() / 2
                                        ),
                                        end = Offset(
                                            size.width,
                                            size.height - bottomBorder.toPx() / 2
                                        ),
                                        strokeWidth = bottomBorder.toPx()
                                    )
                                }
                                if (rightBorder > 0.dp) {
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(size.width - rightBorder.toPx() / 2, 0f),
                                        end = Offset(
                                            size.width - rightBorder.toPx() / 2,
                                            size.height
                                        ),
                                        strokeWidth = rightBorder.toPx()
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.value != 0) {
                            // Mostrar número principal
                            Text(
                                text = cell.value.toString(),
                                fontSize = (30 * scale).sp,
                                color = textColor,
                                fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.Normal
                            )
                        } else if (cell.notes.isNotEmpty()) {
                            // Mostrar notes en una graella 3x3
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(notePadding),  // ← Padding ajustat a mida
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy((0 * noteScale).dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    for (rowNotes in 0 until 3) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly,  // ← SIN espais entre columnes
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (colNotes in 0 until 3) {
                                                val noteNumber = rowNotes * 3 + colNotes + 1
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (cell.notes.contains(
                                                                noteNumber
                                                            )
                                                        ) {
                                                            noteNumber.toString()  // ← Mostra el número
                                                        } else {
                                                            ""  // ← Espai buit però manté la posició
                                                        },
                                                        fontSize = (10 * noteScale).sp,
                                                        color = Color.Gray,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = (8 * noteScale).sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun rememberTimer(
    resetTrigger: Int = 0,
    startOffset: Int = 0,
    isPaused: Boolean = false,
    resetTimer: Int = 0,
    onTimeUpdate: (Int) -> Unit
): String {
    var elapsedTime by remember(resetTrigger, resetTimer) { mutableStateOf(startOffset) }
    // ✅ Guardar el temps quan es pausa
    var timeWhenPaused by remember(resetTrigger) { mutableStateOf(startOffset) }

    LaunchedEffect(resetTrigger, isPaused, resetTimer, startOffset) { // ← Afegir startOffset com a key
        if (!isPaused) {
            // ✅ Començar des del startOffset actualitzat
            val startTime = System.currentTimeMillis() - (startOffset * 1000L)
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (!isPaused) {
                    elapsedTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    timeWhenPaused = elapsedTime
                    onTimeUpdate(elapsedTime)
                }
            }
        } else {
            // ✅ Quan està pausat, mantenir el temps guardat
            elapsedTime = timeWhenPaused
        }
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}




