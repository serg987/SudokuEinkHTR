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



@Composable
fun GameScreen(difficulty: Difficulty, onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()
    var resetTrigger by remember { mutableStateOf(0) }

    // Detectar orientació
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Intentar carregar partida guardada
    val savedGame = remember(resetTrigger) {
        if (resetTrigger == 0) {
            GameStateManager.loadGame(context)?.takeIf {
                it.difficulty == difficulty.name
            }
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
    // Guardar el tauler inicial per poder reiniciar
    val initialBoard = remember(resetTrigger) {
        initialGame.board.map { row ->
            row.map { cell -> cell.copy() }
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
    var showDrawingCanvas by remember { mutableStateOf(false) }

    // Temps inicial (restaurar si hi ha partida guardada)
    var startTimeOffset by remember(resetTrigger) { mutableStateOf(savedGame?.elapsedSeconds ?: 0) }
    val timerText = rememberTimer(resetTrigger, startTimeOffset)

    var hintsRemaining by remember(resetTrigger) {
        mutableStateOf(
            savedGame?.hintsRemaining ?: when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 1
            }
        )
    }

    var isNotesMode by remember(resetTrigger) { mutableStateOf(false) }
    val moveHistory = remember(resetTrigger) { mutableStateListOf<List<List<SudokuCell>>>() }

    // Guardar automàticament cada canvi
    LaunchedEffect(boardState, hintsRemaining) {
        if (!showVictoryDialog) {
            val currentSeconds = timerText.split(":").let {
                it[0].toInt() * 60 + it[1].toInt()
            }

            val savedState = SavedGameState(
                difficulty = difficulty.name,
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
                elapsedSeconds = currentSeconds,
                hintsRemaining = hintsRemaining
            )
            GameStateManager.saveGame(context, savedState)
        }
    }

    // Esborrar partida guardada quan es completa
    LaunchedEffect(showVictoryDialog) {
        if (showVictoryDialog) {
            GameStateManager.clearGame(context)
        }
    }

    // Funció per actualitzar el tauler i guardar a l'historial
    fun updateBoard(newBoard: List<List<SudokuCell>>) {
        moveHistory.add(boardState)
        boardState = newBoard
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

    LaunchedEffect(isComplete, isCorrect) {
        if (isComplete) {
            if (isCorrect) {
                // Registrar estadística
                val timeInSeconds = timerText.split(":").let {
                    it[0].toInt() * 60 + it[1].toInt()
                }
                StatisticsManager.recordCompletion(context, difficulty, timeInSeconds)

                showVictoryDialog = true
            } else {
                showErrorDialog = true
            }
        }
    }

    if (isLandscape) {
        // LAYOUT HORITZONTAL
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // COLUMNA ESQUERRA: Botó Back + Sudoku
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Botó Back a dalt a l'esquerra
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .width(160.dp)
                        .height(50.dp)
                ) {
                    Text(strings.back, fontSize = 28.sp)
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
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp)
                    )

                    Text(text = "⏱️ $timerText", fontSize = 28.sp)
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
                            if (!boardState[row][col].isFixed) {
                                selectedCell = Pair(row, col)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // COLUMNA DRETA: Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Nou Joc
                Button(
                    onClick = {
                        GameStateManager.clearGame(context)
                        resetTrigger++
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(strings.newGame, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // BOTÓ REINICIAR
                Button(
                    onClick = {
                        moveHistory.clear()
                        boardState = initialBoard.map { row ->
                            row.map { cell -> cell.copy() }
                        }
                        selectedCell = null
                        selectedNumber = null
                        startTimeOffset = 0
                        hintsRemaining = when (difficulty) {
                            Difficulty.EASY -> 5
                            Difficulty.MEDIUM -> 3
                            Difficulty.HARD -> 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(strings.reset, fontSize = 28.sp)
                }


                Spacer(modifier = Modifier.height(120.dp))

                // Números (grid 3x3)
                Column {
                    for (rowNum in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (colNum in 0 until 3) {
                                val number = rowNum * 3 + colNum + 1
                                Button(
                                    onClick = {
                                        selectedNumber = number
                                        selectedCell?.let { (row, col) ->
                                            if (!boardState[row][col].isFixed) {
                                                val newBoard = boardState.mapIndexed { r, rowList ->
                                                    rowList.mapIndexed { c, cell ->
                                                        if (r == row && c == col) {
                                                            if (isNotesMode) {
                                                                val newNotes =
                                                                    if (cell.notes.contains(number)) {
                                                                        cell.notes - number
                                                                    } else {
                                                                        cell.notes + number
                                                                    }
                                                                cell.copy(notes = newNotes)
                                                            } else {
                                                                cell.copy(
                                                                    value = number,
                                                                    notes = emptySet()
                                                                )
                                                            }
                                                        } else {
                                                            cell
                                                        }
                                                    }
                                                }
                                                updateBoard(newBoard)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(70.dp)
                                        .padding(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedNumber == number) Color.Black else Color.White,
                                        contentColor = if (selectedNumber == number) Color.White else Color.Black
                                    ),
                                    border = BorderStroke(2.dp, Color.Black)
                                ) {
                                    Text(text = number.toString(), fontSize = 28.sp)
                                }
                            }
                        }
                        // Afegir espai entre files (excepte després de l'última)
                        if (rowNum < 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5 botons en columna
                Button(
                    onClick = { showDrawingCanvas = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(strings.pencil, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { isNotesMode = !isNotesMode },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNotesMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isNotesMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(if (isNotesMode) strings.notesOn else strings.notesOff, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
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
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(strings.erase, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (hintsRemaining > 0) {
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("${strings.hint} ($hintsRemaining)", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (moveHistory.isNotEmpty()) {
                            boardState = moveHistory.removeAt(moveHistory.lastIndex)
                        }
                    },
                    enabled = moveHistory.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.undo, fontSize = 28.sp)
                }
            }
        }
    } else {
        // LAYOUT VERTICAL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

        // FILA SUPERIOR: Botó tornar (esquerra) i Nou Joc (dreta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .height(50.dp)
                        .width(160.dp)
                ) {
                    Text(
                        strings.back,
                        fontSize = 28.sp
                    )
                }

                Button(
                    onClick = {
                        GameStateManager.clearGame(context)
                        resetTrigger++
                    },
                    modifier = Modifier
                        .height(50.dp)
                        .width(180.dp)
                ) {
                    Text(
                        strings.newGame,
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            // FILA AMB NIVELL/TIMER CENTRAT I BOTÓ REINICIAR A LA DRETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(140.dp)) // Espai per equilibrar

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 28.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "⏱️ $timerText",
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = {
                        moveHistory.clear()
                        boardState = initialBoard.map { row ->
                            row.map { cell -> cell.copy() }
                        }
                        selectedCell = null
                        selectedNumber = null
                        startTimeOffset = 0
                        hintsRemaining = when (difficulty) {
                            Difficulty.EASY -> 5
                            Difficulty.MEDIUM -> 3
                            Difficulty.HARD -> 1
                        }
                    },
                    modifier = Modifier
                        .height(50.dp)
                        .width(240.dp)
                ) {
                    Text(strings.reset, fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SudokuBoard(
                board = boardState,
                solution = solution,
                selectedCell = selectedCell,
                onCellClick = { row, col ->
                    if (!boardState[row][col].isFixed) {
                        selectedCell = Pair(row, col)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Només els números (sense botó de notes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (number in 1..9) {
                    Button(
                        onClick = {
                            selectedNumber = number
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed) {
                                    val newBoard = boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                if (isNotesMode) {
                                                    val newNotes =
                                                        if (cell.notes.contains(number)) {
                                                            cell.notes - number
                                                        } else {
                                                            cell.notes + number
                                                        }
                                                    cell.copy(notes = newNotes)
                                                } else {
                                                    cell.copy(value = number, notes = emptySet())
                                                }
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                    updateBoard(newBoard)
                                }
                            }
                        },
                        modifier = Modifier.size(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedNumber == number) Color.Black else Color.White,
                            contentColor = if (selectedNumber == number) Color.White else Color.Black
                        ),
                        border = BorderStroke(2.dp, Color.Black)
                    ) {
                        Text(
                            text = number.toString(),
                            fontSize = 28.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FILA DE 4 BOTONS: Notes, Esborrar, Pista, Desfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // BOTÓ LLAPIS (NOU)
                Button(
                    onClick = { showDrawingCanvas = true },
                    modifier = Modifier
                        .height(55.dp)
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.pencil, fontSize = 20.sp)
                }

                // BOTÓ NOTES
                Button(
                    onClick = { isNotesMode = !isNotesMode },
                    modifier = Modifier
                        .height(55.dp)
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNotesMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isNotesMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = if (isNotesMode) strings.notesOn else strings.notesOff,
                        fontSize = 20.sp
                    )
                }

                // BOTÓ ESBORRAR
                Button(
                    onClick = {
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
                    },
                    modifier = Modifier
                        .height(55.dp)
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.erase, fontSize = 20.sp)
                }

                // BOTÓ PISTA
                Button(
                    onClick = {
                        if (hintsRemaining > 0) {
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
                        .height(55.dp)
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("${strings.hint} ($hintsRemaining)", fontSize = 20.sp)
                }

                // BOTÓ DESFER
                Button(
                    onClick = {
                        if (moveHistory.isNotEmpty()) {
                            boardState = moveHistory.removeAt(moveHistory.lastIndex)
                        }
                    },
                    enabled = moveHistory.isNotEmpty(),
                    modifier = Modifier
                        .height(55.dp)
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.undo, fontSize = 20.sp)
                }
            }
        }
    }
        // Diàleg de canvas de dibuix
        if (showDrawingCanvas) {
            AlertDialog(
                onDismissRequest = { showDrawingCanvas = false },
                title = { Text("Reconeixement de dígits", fontSize = 24.sp) },
                text = {
                    DrawingCanvas(
                        onDigitRecognized = { digit ->
                            // Col·locar el dígit reconegut a la cel·la seleccionada
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed && digit in 1..9) {
                                    val newBoard = boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                if (isNotesMode) {
                                                    // Mode notes: afegir/treure nota
                                                    val newNotes = if (cell.notes.contains(digit)) {
                                                        cell.notes - digit
                                                    } else {
                                                        cell.notes + digit
                                                    }
                                                    cell.copy(notes = newNotes)
                                                } else {
                                                    // Mode normal: posar número
                                                    cell.copy(value = digit, notes = emptySet())
                                                }
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                    updateBoard(newBoard)
                                }
                            }
                            showDrawingCanvas = false
                        },
                        onDismiss = { showDrawingCanvas = false }
                    )
                },
                confirmButton = {}
            )
        }
        // Diàleg de victòria
        if (showVictoryDialog) {
            AlertDialog(
                onDismissRequest = { showVictoryDialog = false },
                title = {
                    Text(
                        text = strings.congratulations,
                        fontSize = 32.sp
                    )
                },
                text = {
                    Text(
                        text = "${strings.completed}\n\n${strings.time} $timerText",
                        fontSize = 32.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(strings.backToMenu, fontSize = 24.sp)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showVictoryDialog = false },
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(strings.continue_, fontSize = 24.sp)
                    }
                }
            )
        }

        // Diàleg d'error
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text(
                        text = strings.error,
                        fontSize =32.sp,
                        color = Color.Red
                    )
                },
                text = {
                    Text(
                        text = strings.errorMessage,
                        fontSize = 32.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog = false },
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(strings.review, fontSize = 24.sp)
                    }
                }
            )
        }
    }

@Composable
fun SudokuBoard(
    board: List<List<SudokuCell>>,
    solution: List<List<Int>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border(3.dp, Color.Black)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 9) {
                    val cell = board[row][col]
                    val isSelected = selectedCell == Pair(row, col)

                    // Determinar el color del text
                    val textColor = when {
                        cell.isFixed -> Color.Black  // Números inicials
                        cell.value == 0 -> Color.Black  // Cel·la buida
                        else -> Color.Blue  // Tots els números de l'usuari en blau
                    }

                    val topBorder = when {
                        row == 3 || row == 6 -> 2.dp
                        else -> 0.5.dp
                    }
                    val leftBorder = when {
                        col == 3 || col == 6 -> 2.dp
                        else -> 0.5.dp
                    }
                    val bottomBorder = if (row == 8) 0.dp else 0.5.dp
                    val rightBorder = if (col == 8) 0.dp else 0.5.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    isSelected -> Color(0xFFCCCCCC)
                                    cell.isFixed -> Color.White
                                    else -> Color(0xFFF5F5F5)
                                }
                            )
                            .clickable { onCellClick(row, col) }
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
                                        start = Offset(0f, size.height - bottomBorder.toPx() / 2),
                                        end = Offset(size.width, size.height - bottomBorder.toPx() / 2),
                                        strokeWidth = bottomBorder.toPx()
                                    )
                                }
                                if (rightBorder > 0.dp) {
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(size.width - rightBorder.toPx() / 2, 0f),
                                        end = Offset(size.width - rightBorder.toPx() / 2, size.height),
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
                                fontSize = 36.sp,
                                color = textColor,
                                fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.Normal
                            )
                        } else if (cell.notes.isNotEmpty()) {
                            // Mostrar notes en una graella 3x3
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    for (rowNotes in 0 until 3) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (colNotes in 0 until 3) {
                                                val noteNumber = rowNotes * 3 + colNotes + 1
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (cell.notes.contains(noteNumber)) {
                                                        Text(
                                                            text = noteNumber.toString(),
                                                            fontSize = 32.sp,
                                                            color = Color.Gray
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
}

@Composable
fun rememberTimer(resetTrigger: Int = 0, startOffset: Int = 0): String {
    var elapsedTime by remember(resetTrigger) { mutableStateOf(startOffset) }

    LaunchedEffect(resetTrigger) {
        val startTime = System.currentTimeMillis() - (startOffset * 1000L)
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        }
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
