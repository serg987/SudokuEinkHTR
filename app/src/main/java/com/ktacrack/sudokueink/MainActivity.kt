package com.ktacrack.sudokueink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ktacrack.sudokueink.ui.theme.SudokuEinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aplicar optimitzacions e-ink
        EinkOptimizations.disableAnimations(window)
        EinkOptimizations.setOnyxRefreshMode(window, 2) // Mode de qualitat

        setContent {
            val context = LocalContext.current
            var isDarkTheme by remember { mutableStateOf(ThemeManager.loadDarkMode(context)) }

            // Observar canvis del tema
            LaunchedEffect(Unit) {
                // Recarregar tema cada cop que es reprèn l'activitat
            }

            SudokuEinkTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DisableCompositionAnimations()
                    AppNavigation(
                        onThemeChange = { newIsDark ->
                            isDarkTheme = newIsDark
                        }
                    )
                }
            }
        }
    }
}
