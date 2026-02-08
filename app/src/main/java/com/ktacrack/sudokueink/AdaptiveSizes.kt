package com.ktacrack.sudokueink

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

object AdaptiveSizes {

    /**
     * Calcula un factor d'escala basat en la mida de la pantalla i orientació
     * - Vertical: Referència 393x852 dp (mòbil estàndard)
     * - Horitzontal: Referència 800x480 dp (tablet)
     */
    @Composable
    fun getScaleFactor(): Float {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        return if (isLandscape) {
            // Horitzontal: tablet referència
            minOf(screenWidthDp / 950f, screenHeightDp / 550f)
        } else {
            // Vertical: compromís entre mòbils grans i tablets petites
            minOf(screenWidthDp / 800f, screenHeightDp / 1100f)
        }.coerceIn(0.60f, 1.15f)
    }
}
