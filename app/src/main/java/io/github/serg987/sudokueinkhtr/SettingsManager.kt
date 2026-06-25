package io.github.serg987.sudokueinkhtr

import android.content.Context
import android.content.SharedPreferences

enum class HtrModel {
    TFLITE, ONNX, MLKIT
}

object SettingsManager {
    private const val PREFS_NAME = "sudoku_settings"
    private const val KEY_IS_PENCIL = "is_pencil"
    private const val KEY_INK_THICKNESS = "ink_thickness"
    private const val KEY_HTR_MODEL = "htr_model"
    private const val KEY_ZEN_MODE = "zen_mode"
    private const val KEY_DIFFICULTY = "difficulty"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDifficulty(context: Context, difficulty: Difficulty) {
        getPrefs(context).edit().putString(KEY_DIFFICULTY, difficulty.name).apply()
    }

    fun loadDifficulty(context: Context): Difficulty {
        val diffName = getPrefs(context).getString(KEY_DIFFICULTY, Difficulty.EASY.name)
        return try {
            Difficulty.valueOf(diffName ?: Difficulty.EASY.name)
        } catch (e: Exception) {
            Difficulty.EASY
        }
    }

    fun saveIsPencil(context: Context, isPencil: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_PENCIL, isPencil).apply()
    }

    fun loadIsPencil(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PENCIL, true)
    }

    fun saveInkThickness(context: Context, thickness: Float) {
        getPrefs(context).edit().putFloat(KEY_INK_THICKNESS, thickness).apply()
    }

    fun loadInkThickness(context: Context): Float {
        return getPrefs(context).getFloat(KEY_INK_THICKNESS, 9f)
    }

    fun saveHtrModel(context: Context, model: HtrModel) {
        getPrefs(context).edit().putString(KEY_HTR_MODEL, model.name).apply()
    }

    fun loadHtrModel(context: Context): HtrModel {
        val modelName = getPrefs(context).getString(KEY_HTR_MODEL, HtrModel.TFLITE.name)
        return try {
            HtrModel.valueOf(modelName ?: HtrModel.TFLITE.name)
        } catch (e: Exception) {
            HtrModel.TFLITE
        }
    }

    fun saveZenMode(context: Context, isZenMode: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ZEN_MODE, isZenMode).apply()
    }

    fun loadZenMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ZEN_MODE, false)
    }
}
