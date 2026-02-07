package com.ktacrack.sudokueink

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawingCanvas(
    onDigitRecognized: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }

    val recognizer = remember { DigitRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Dibuixa un número:", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas de dibuix
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                        },
                        onDrag = { change, _ ->
                            currentPath.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            paths = paths + currentPath
                            currentPath = Path()
                        }
                    )
                }
        ) {
            // Fons NEGRE
            drawRect(Color.Black)

            // Dibuixar tots els paths en BLANC
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 20f)
                )
            }

            // Dibuixar path actual en BLANC
            drawPath(
                path = currentPath,
                color = Color.White,
                style = Stroke(width = 20f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botó Esborrar
            Button(onClick = {
                paths = emptyList()
                currentPath = Path()
            }) {
                Text("Esborrar", fontSize = 20.sp)
            }

            // Botó Reconèixer
            Button(onClick = {
                if (paths.isNotEmpty()) {
                    // Convertir canvas a bitmap
                    val bitmap = pathsToBitmap(paths, with(density) { 300.dp.toPx().toInt() })
                    val digit = recognizer.recognizeDigit(bitmap)
                    onDigitRecognized(digit)
                }
            }) {
                Text("Reconèixer", fontSize = 20.sp)
            }

            // Botó Cancel·lar
            Button(onClick = onDismiss) {
                Text("Cancel·lar", fontSize = 20.sp)
            }
        }
    }
}

private fun pathsToBitmap(paths: List<Path>, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.BLACK)  // Fons NEGRE

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE  // Traç BLANC
        strokeWidth = 20f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
    }

    paths.forEach { path ->
        canvas.drawPath(path.asAndroidPath(), paint)
    }

    return bitmap
}
