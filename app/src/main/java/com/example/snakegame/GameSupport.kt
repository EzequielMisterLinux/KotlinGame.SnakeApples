package com.example.snakegame

import android.graphics.Point

// Interface for game callbacks
interface GameCallbacks {
    fun onGameOver(success: Boolean)
    fun onScoreIncreased(points: Int)
}

// Enum class for direction
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

// Data class for apple
data class Apple(
    val position: Point,
    val color: Int
)