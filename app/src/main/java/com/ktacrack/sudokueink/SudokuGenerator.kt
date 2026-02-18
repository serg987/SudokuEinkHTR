package com.ktacrack.sudokueink

import kotlin.random.Random

object SudokuGenerator {

    fun generate(difficulty: Difficulty, seed: Int? = null): SudokuGame {
        val random = if (seed != null) Random(seed) else Random.Default

        // Generar una solució completament aleatòria des de zero
        val solution = generateRandomSolution(random)

        // Determinar quantes caselles deixem buides
        val cellsToRemove = when (difficulty) {
            Difficulty.EASY -> 30
            Difficulty.MEDIUM -> 40
            Difficulty.HARD -> 50
        }

        // Crear el puzzle amb verificació de solució única
        val board = createPuzzleWithUniqueCheck(solution, cellsToRemove, random)
        return SudokuGame(board, solution)
    }

    // ✅ NOVA: Genera una solució completament aleatòria
    private fun generateRandomSolution(random: Random): List<List<Int>> {
        val board = MutableList(9) { MutableList(9) { 0 } }
        fillBoardRandomly(board, 0, 0, random)
        return board.map { it.toList() }
    }

    // ✅ NOVA: Emplena el tauler amb backtracking aleatori
    private fun fillBoardRandomly(
        board: MutableList<MutableList<Int>>,
        row: Int,
        col: Int,
        random: Random
    ): Boolean {
        // Si hem arribat al final del tauler, hem acabat
        if (row == 9) return true

        // Calcular següent posició
        val nextRow = if (col == 8) row + 1 else row
        val nextCol = if (col == 8) 0 else col + 1

        // Crear llista de números 1-9 en ordre aleatori  (sempre  igual si Daily)
        val numbers = (1..9).shuffled(random)

        // Provar cada número
        for (num in numbers) {
            if (isValidPlacement(board, row, col, num)) {
                board[row][col] = num

                // Recursivament emplenar la següent casella
                if (fillBoardRandomly(board, nextRow, nextCol, random)) {
                    return true
                }

                // Backtrack si no funciona
                board[row][col] = 0
            }
        }

        return false
    }

    // ✅ NOVA: Comprova si un número es pot col·locar en una posició
    private fun isValidPlacement(
        board: List<List<Int>>,
        row: Int,
        col: Int,
        num: Int,
    ): Boolean {
        // Comprovar fila
        if (board[row].contains(num)) return false

        // Comprovar columna
        for (r in 0 until 9) {
            if (board[r][col] == num) return false
        }

        // Comprovar caixa 3x3
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board[r][c] == num) return false
            }
        }

        return true
    }

    // Crea puzzle verificant que tingui solució única
    private fun createPuzzleWithUniqueCheck(
        solution: List<List<Int>>,
        cellsToRemove: Int,
        random: Random
    ): List<List<SudokuCell>> {
        val board = solution.map { row ->
            row.map { value ->
                SudokuCell(value = value, isFixed = true)
            }.toMutableList()
        }.toMutableList()

        // Llista de totes les posicions en ordre aleatori
        val positions = (0 until 81).shuffled(random).toMutableList()
        var removed = 0
        var attemptIndex = 0

        while (removed < cellsToRemove && attemptIndex < positions.size) {
            val pos = positions[attemptIndex]
            val row = pos / 9
            val col = pos % 9

            // Guardar valor abans de treure'l
            val backup = board[row][col].value

            if (backup != 0) {
                // Intentar treure el número
                board[row][col] = SudokuCell(value = 0, isFixed = false)

                // Verificar que només hi ha 1 solució
                if (hasUniqueSolution(board)) {
                    removed++
                } else {
                    // Si té múltiples solucions, tornar a posar el número
                    board[row][col] = SudokuCell(value = backup, isFixed = true)
                }
            }

            attemptIndex++
        }

        // Convertir a llistes immutables
        return board.map { it.toList() }
    }

    // Comprova si el puzzle té solució única
    private fun hasUniqueSolution(board: List<List<SudokuCell>>): Boolean {
        // Convertir a format de solver
        val puzzle = board.map { row ->
            row.map { cell -> cell.value }.toMutableList()
        }.toMutableList()

        // Comptar solucions (màxim 2 per eficiència)
        val solutionCount = countSolutions(puzzle, 0, 0, 0)
        return solutionCount == 1
    }

    // Compta quantes solucions té un puzzle (màxim 2)
    private fun countSolutions(
        board: MutableList<MutableList<Int>>,
        row: Int,
        col: Int,
        count: Int
    ): Int {
        // Si ja hem trobat 2 solucions, parar
        if (count >= 2) return count

        // Trobar la següent casella buida
        var r = row
        var c = col
        var found = false

        outer@ for (i in r until 9) {
            for (j in (if (i == r) c else 0) until 9) {
                if (board[i][j] == 0) {
                    r = i
                    c = j
                    found = true
                    break@outer
                }
            }
        }

        // Si no hi ha més caselles buides, hem trobat una solució
        if (!found) return count + 1

        // Provar tots els números de 1 a 9
        var currentCount = count
        for (num in 1..9) {
            if (isValidMove(board, r, c, num)) {
                board[r][c] = num
                currentCount = countSolutions(board, r, c + 1, currentCount)
                board[r][c] = 0

                // Si ja hem trobat 2 solucions, parar
                if (currentCount >= 2) break
            }
        }

        return currentCount
    }

    // Comprova si un número és vàlid en una posició (per solver)
    private fun isValidMove(
        board: List<List<Int>>,
        row: Int,
        col: Int,
        num: Int
    ): Boolean {
        // Comprovar fila
        if (board[row].contains(num)) return false

        // Comprovar columna
        if ((0 until 9).any { board[it][col] == num }) return false

        // Comprovar caixa 3x3
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board[r][c] == num) return false
            }
        }

        return true
    }
}
