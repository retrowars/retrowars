package com.serwylo.retrowars.games.snake

import java.util.*

class SnakeGameState() {

    companion object {
        const val CELLS_WIDE = 29
        const val CELLS_HIGH = 19
    }

    var timeStep = 0.15f
    val minTimeStep = 0.075f
    var nextTimeStep = timeStep
    var timer = 0f

    /**
     * If this is greater than one, then we will extend the length of the snake for this many time
     * steps regardless of whether you have eaten anything or not.
     */
    var queuedGrowth = 0

    var cells: List<List<Cell>>
    var score: Long = 0

    val snake = LinkedList<Cell>()
    val obstacles = mutableListOf<Cell>()
    var food: Cell? = null

    // TODO: Speed

    // TODO: Direction (including dealing with multitouch sensibly as we can only move one direction)

    var left = false
    var right = false
    var up = false
    var down = false

    var currentDirection = Direction.UP
    var nextDirection = Direction.UP

    init {
        cells = (0 until CELLS_HIGH).map { y ->
            (0 until CELLS_WIDE).map { x ->
                Cell(x, y)
            }
        }

        snake.add(cells[CELLS_HIGH / 2][CELLS_WIDE / 2])
        food = cells[CELLS_HIGH * 2 / 3][CELLS_WIDE / 2]
    }

    class Cell(val x: Int, val y: Int) {
        override fun toString() = "($x, $y)"
    }

}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
