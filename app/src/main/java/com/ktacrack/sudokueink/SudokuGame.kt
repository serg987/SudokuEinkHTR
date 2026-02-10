package com.ktacrack.sudokueink

data class SudokuCell(
    val value: Int,        // 0 = buit, 1-9 = número
    val isFixed: Boolean,  // true = número inicial (no editable)
    val notes: Set<Int> = emptySet()  // notes mode borrador
)

data class SudokuGame(
    val board: List<List<SudokuCell>>,  // 9x9
    val solution: List<List<Int>>       // solució correcta
)

enum class Difficulty {
    EASY, MEDIUM, HARD
}

enum class GameMode {
    NORMAL,
    ATTACK
}

