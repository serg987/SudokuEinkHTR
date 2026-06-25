package io.github.serg987.sudokueinkhtr

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedGameState(
    val difficulty: String,
    val mode: String,
    val board: List<List<SavedCell>>,
    val solution: List<List<Int>>,
    val elapsedSeconds: Int,
    val hintsRemaining: Int,
    val moveCount: Int = 0
)

@Serializable
data class SavedCell(
    val value: Int,
    val isFixed: Boolean,
    val notes: List<Int>,
    val isPencil: Boolean = false,
    val strokes: List<List<DrawingPoint>> = emptyList()
)

object GameStateManager {
    private const val PREFS_NAME = "sudoku_games"  // ✅ UNIFICAT

    fun saveGame(context: Context, savedState: SavedGameState, isDaily: Boolean, isZenMode: Boolean) {
        val gameKey = "game_${savedState.difficulty}_${savedState.mode}_${if(isDaily) "daily" else if(isZenMode) "zen" else "normal"}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(savedState)  // ✅ CORREGIT: savedState

        prefs.edit().putString("game_$gameKey", json).apply()
    }

    fun loadGame(context: Context, mode: GameMode, difficulty: Difficulty, isDaily: Boolean, isZenMode: Boolean): SavedGameState? {
        val gameKey = "game_${difficulty}_${mode}_${if(isDaily) "daily" else if(isZenMode) "zen" else "normal"}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("game_$gameKey", null) ?: return null

        return try {
            Json.decodeFromString<SavedGameState>(json)  // ✅ Retorna SavedGameState
        } catch (e: Exception) {
            null
        }
    }

    fun clearGame(context: Context, mode: GameMode, difficulty: Difficulty, isDaily: Boolean, isZenMode: Boolean) {
        val gameKey = "game_${difficulty}_${mode}_${if(isDaily) "daily" else if(isZenMode) "zen" else "normal"}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("game_$gameKey").apply()
    }

    fun hasSavedGame(context: Context, mode: GameMode, difficulty: Difficulty, isDaily: Boolean = false, isZenMode: Boolean = false): Boolean {
        val gameKey = "game_${difficulty}_${mode}_${if(isDaily) "daily" else if(isZenMode) "zen" else "normal"}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains("game_$gameKey")
    }

    fun clearAllGames(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
