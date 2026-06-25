package io.github.serg987.sudokueinkhtr

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

object EinkOptimizations {

    // Desactiva les animacions del sistema (versió moderna)
    fun disableAnimations(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Desactiva animacions de transicions
        window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    // Forçar refresc complet de pantalla (per Onyx Boox)
    fun forceFullRefresh(view: View) {
        try {
            val method = view.javaClass.getMethod("postInvalidate")
            method.invoke(view)
        } catch (e: Exception) {
            // Si no és un dispositiu Boox, ignorar
            view.postInvalidate()
        }
    }

    // Optimització específica per Onyx Boox
    fun setOnyxRefreshMode(window: Window, mode: Int) {
        try {
            // Mode 0: Actualització normal
            // Mode 1: Actualització ràpida (A2 mode)
            // Mode 2: Actualització de qualitat (GC16)
            val decorView = window.decorView
            val method = decorView.javaClass.getMethod("setDefaultUpdateMode", Int::class.java)
            method.invoke(decorView, mode)
        } catch (e: Exception) {
            // No és un dispositiu Onyx o no suporta aquesta funció
        }
    }
}

@Composable
fun DisableCompositionAnimations() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        EinkOptimizations.forceFullRefresh(view)
        onDispose { }
    }
}
