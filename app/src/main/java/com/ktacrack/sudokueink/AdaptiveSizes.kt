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
            minOf(screenWidthDp / 950f, screenHeightDp / 550f)  // ← De 800/480 a 900/540
        } else {
            minOf(screenWidthDp / 550f, screenHeightDp / 950f)  // ← De 393/852 a 450/950
        }.coerceAtMost(1.15f)
    }
}
