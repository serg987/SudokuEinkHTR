package io.github.serg987.sudokueinkhtr

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.serg987.sudokueinkhtr.ui.theme.SudokuEinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Forçar vertical en pantalles petites (mòbils)
        val smallestWidthDp = resources.configuration.smallestScreenWidthDp
        if (smallestWidthDp < 600) {  // Mòbils: només vertical
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {  // Tablets: rotació lliure
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

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
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
