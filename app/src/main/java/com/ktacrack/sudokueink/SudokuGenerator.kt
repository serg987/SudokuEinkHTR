package com.ktacrack.sudokueink

import kotlin.random.Random

object SudokuGenerator {

    fun generate(difficulty: Difficulty): SudokuGame {
        // Generem una solució vàlida
        val solution = generateSolution()

        // Determinem quantes caselles deixem buides
        val cellsToRemove = when (difficulty) {
            Difficulty.EASY -> 30
            Difficulty.MEDIUM -> 40
            Difficulty.HARD -> 50
        }

        // Creem el tauler amb caselles buides
        val board = createPuzzle(solution, cellsToRemove)

        return SudokuGame(board, solution)
    }

    private fun generateSolution(): List<List<Int>> {
        // Solució base vàlida
        val base = listOf(
            listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
            listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
            listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
            listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
            listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
            listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
            listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
            listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
            listOf(3, 4, 5, 2, 8, 6, 1, 7, 9)
        )

        // Barregem files dins de cada banda de 3
        val shuffled = base.toMutableList()

        // Barregem files dins de la banda superior (0-2)
        val topBand = shuffled.subList(0, 3).shuffled()
        for (i in 0 until 3) shuffled[i] = topBand[i]

        // Barregem files dins de la banda central (3-5)
        val midBand = shuffled.subList(3, 6).shuffled()
        for (i in 3 until 6) shuffled[i] = midBand[i - 3]

        // Barregem files dins de la banda inferior (6-8)
        val botBand = shuffled.subList(6, 9).shuffled()
        for (i in 6 until 9) shuffled[i] = botBand[i - 6]

        // Barregem les bandes completes
        val bands = listOf(
            shuffled.subList(0, 3),
            shuffled.subList(3, 6),
            shuffled.subList(6, 9)
        ).shuffled()

        return bands.flatten()
    }

    private fun createPuzzle(
        solution: List<List<Int>>,
        cellsToRemove: Int
    ): List<List<SudokuCell>> {
        val board = solution.map { row ->
            row.map { value ->
                SudokuCell(value = value, isFixed = true)
            }.toMutableList()
        }.toMutableList()

        // Treiem caselles aleatòriament
        var removed = 0
        while (removed < cellsToRemove) {
            val row = Random.nextInt(9)
            val col = Random.nextInt(9)

            if (board[row][col].value != 0) {
                board[row][col] = SudokuCell(value = 0, isFixed = false)
                removed++
            }
        }

        // Convertim a llistes immutables
        return board.map { it.toList() }
    }
}