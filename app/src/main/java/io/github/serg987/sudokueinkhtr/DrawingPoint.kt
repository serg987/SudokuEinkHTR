package io.github.serg987.sudokueinkhtr

import kotlinx.serialization.Serializable

@Serializable
data class DrawingPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = 0L
)
