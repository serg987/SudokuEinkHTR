package com.ktacrack.sudokueink

import android.content.Context

object ThemeManager {
    private const val PREFS_NAME = "sudoku_theme"
    private const val KEY_DARK_MODE = "dark_mode"

    fun saveDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun loadDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false) // Per defecte tema clar
    }
}
