package com.ktacrack.sudokueink

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedGameState(
    val difficulty: String,
    val board: List<List<SavedCell>>,
    val solution: List<List<Int>>,
    val elapsedSeconds: Int,
    val hintsRemaining: Int
)

@Serializable
data class SavedCell(
    val value: Int,
    val isFixed: Boolean,
    val notes: List<Int>
)

object GameStateManager {
    private const val PREFS_NAME = "sudoku_game_state"

    // Claus específiques per cada dificultat
    private fun getKeyForDifficulty(difficulty: Difficulty): String {
        return "game_state_${difficulty.name}"
    }

    fun saveGame(context: Context, state: SavedGameState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(state)

        // Convertir String a Difficulty
        val difficulty = when (state.difficulty) {
            "EASY" -> Difficulty.EASY
            "MEDIUM" -> Difficulty.MEDIUM
            "HARD" -> Difficulty.HARD
            else -> return
        }

        // Guardar amb clau específica
        val key = getKeyForDifficulty(difficulty)
        prefs.edit().putString(key, json).apply()
    }

    fun loadGame(context: Context, difficulty: Difficulty): SavedGameState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getKeyForDifficulty(difficulty)
        val json = prefs.getString(key, null) ?: return null

        return try {
            Json.decodeFromString<SavedGameState>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearGame(context: Context, difficulty: Difficulty) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getKeyForDifficulty(difficulty)
        prefs.edit().remove(key).apply()
    }

    // Funció opcional per esborrar TOTES les partides
    fun clearAllGames(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
