package com.ktacrack.sudokueink

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


enum class Language(val code: String) {
    CATALAN("ca"),
    SPANISH("es"),
    ENGLISH("en")
}

object LanguageManager {
    private const val PREFS_NAME = "sudoku_language"
    private const val KEY_LANGUAGE = "language"

    fun saveLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun loadLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, Language.CATALAN.code) ?: Language.CATALAN.code
        return Language.values().find { it.code == code } ?: Language.CATALAN
    }
}

@Composable
fun rememberStrings(): Strings {
    val context = LocalContext.current
    val currentLanguage = remember { LanguageManager.loadLanguage(context) }

    return remember(currentLanguage) {
        when (currentLanguage) {
            Language.CATALAN -> StringsCa
            Language.SPANISH -> StringsEs
            Language.ENGLISH -> StringsEn
        }
    }
}

interface Strings {
    // Menu
    val appTitle: String
    val difficultyEasy: String
    val difficultyMedium: String
    val difficultyHard: String
    val statistics: String
    val createdBy: String
    val settings: String

    // Game
    val back: String
    val newGame: String
    val reset: String
    val pencil: String
    val notesOn: String
    val notesOff: String
    val erase: String
    val hint: String
    val undo: String

    // Dialogs
    val congratulations: String
    val completed: String
    val time: String
    val backToMenu: String
    val continue_: String
    val error: String
    val errorMessage: String
    val review: String

    // Statistics
    val statisticsTitle: String
    val gamesCompleted: String
    val bestTime: String
    val totalGames: String

    // Settings
    val settingsTitle: String
    val language: String
    val catalan: String
    val spanish: String
    val english: String

    // Theme
    val theme: String
    val lightTheme: String
    val darkTheme: String
}

object StringsCa : Strings {
    override val appTitle = "Sudoku E-ink"
    override val difficultyEasy = "Nivell Fàcil"
    override val difficultyMedium = "Nivell Intermedi"
    override val difficultyHard = "Nivell Difícil"
    override val statistics = "📊 Estadístiques"
    override val settings = "⚙️ Configuració"
    override val createdBy = "Creada per ktacrack"

    override val back = "← Tornar"
    override val newGame = "🔄 Nou Joc"
    override val reset = "Reiniciar Sudoku"
    override val pencil = "Llapis"
    override val notesOn = "Notes ON"
    override val notesOff = "Notes OFF"
    override val erase = "Esborrar"
    override val hint = "Pista"
    override val undo = "Desfer"

    override val congratulations = "🎉 Felicitats!"
    override val completed = "Has completat el Sudoku correctament!"
    override val time = "Temps:"
    override val backToMenu = "Tornar al menú"
    override val continue_ = "Continuar"
    override val error = "❌ Hi ha errors!"
    override val errorMessage = "El tauler està complet però té números incorrectes. Revisa les cel·les!"
    override val review = "Revisar"

    override val statisticsTitle = "📊 Estadístiques"
    override val gamesCompleted = "Partides completades:"
    override val bestTime = "Millor temps:"
    override val totalGames = "Total de partides completades"

    override val theme = "Tema"
    override val lightTheme = "Clar"
    override val darkTheme = "Fosc"

    override val settingsTitle = "⚙️ Configuració"
    override val language = "Idioma"
    override val catalan = "Català"
    override val spanish = "Castellà"
    override val english = "Anglès"
}

object StringsEs : Strings {
    override val appTitle = "Sudoku E-ink"
    override val difficultyEasy = "Nivel Fácil"
    override val difficultyMedium = "Nivel Intermedio"
    override val difficultyHard = "Nivel Difícil"
    override val statistics = "📊 Estadísticas"
    override val settings = "⚙️ Configuración"
    override val createdBy = "Creada por ktacrack"

    override val back = "← Volver"
    override val newGame = "🔄 Nuevo Juego"
    override val reset = "Reiniciar Sudoku"
    override val pencil = "Lápiz"
    override val notesOn = "Notas ON"
    override val notesOff = "Notas OFF"
    override val erase = "Borrar"
    override val hint = "Pista"
    override val undo = "Deshacer"

    override val congratulations = "🎉 ¡Felicidades!"
    override val completed = "¡Has completado el Sudoku correctamente!"
    override val time = "Tiempo:"
    override val backToMenu = "Volver al menú"
    override val continue_ = "Continuar"
    override val error = "❌ ¡Hay errores!"
    override val errorMessage = "El tablero está completo pero tiene números incorrectos. ¡Revisa las celdas!"
    override val review = "Revisar"

    override val statisticsTitle = "📊 Estadísticas"
    override val gamesCompleted = "Partidas completadas:"
    override val bestTime = "Mejor tiempo:"
    override val totalGames = "Total de partidas completadas"

    override val theme = "Tema"
    override val lightTheme = "Claro"
    override val darkTheme = "Oscuro"

    override val settingsTitle = "⚙️ Configuración"
    override val language = "Idioma"
    override val catalan = "Catalán"
    override val spanish = "Castellano"
    override val english = "Inglés"
}

object StringsEn : Strings {
    override val appTitle = "Sudoku E-ink"
    override val difficultyEasy = "Level Easy"
    override val difficultyMedium = "Level Medium"
    override val difficultyHard = "Level Hard"
    override val statistics = "📊 Statistics"
    override val settings = "⚙️ Settings"
    override val createdBy = "Created by ktacrack"

    override val back = "← Back"
    override val newGame = "🔄 New Game"
    override val reset = "Reset Sudoku"
    override val pencil = "Pencil"
    override val notesOn = "Notes ON"
    override val notesOff = "Notes OFF"
    override val erase = "Erase"
    override val hint = "Hint"
    override val undo = "Undo"

    override val congratulations = "🎉 Congratulations!"
    override val completed = "You have completed the Sudoku correctly!"
    override val time = "Time:"
    override val backToMenu = "Back to menu"
    override val continue_ = "Continue"
    override val error = "❌ There are errors!"
    override val errorMessage = "The board is complete but has incorrect numbers. Review the cells!"
    override val review = "Review"

    override val statisticsTitle = "📊 Statistics"
    override val gamesCompleted = "Games completed:"
    override val bestTime = "Best time:"
    override val totalGames = "Total games completed"

    override val theme = "Theme"
    override val lightTheme = "Light"
    override val darkTheme = "Dark"

    override val settingsTitle = "⚙️ Settings"
    override val language = "Language"
    override val catalan = "Catalan"
    override val spanish = "Spanish"
    override val english = "English"
}
