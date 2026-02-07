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
    private const val KEY_GAME_STATE = "current_game"

    fun saveGame(context: Context, state: SavedGameState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(state)
        prefs.edit().putString(KEY_GAME_STATE, json).apply()
    }

    fun loadGame(context: Context): SavedGameState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_GAME_STATE, null) ?: return null
        return try {
            Json.decodeFromString<SavedGameState>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearGame(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_GAME_STATE).apply()
    }
}
