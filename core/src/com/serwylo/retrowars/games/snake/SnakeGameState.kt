package com.serwylo.retrowars.games.snake

import java.util.*

class SnakeGameState() {

    var timeStep = 0.5f
    val minTimeStep = 0.075f
    var nextTimeStep = timeStep
    var timer = 0f

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
        cells = (0 until 30).map { y ->
            (0 until 30).map { x ->
                Cell(x, y)
            }
        }

        snake.add(cells[15][15])

        food = cells[20][15]
    }

    class Cell(val x: Int, val y: Int) {
        override fun toString() = "($x, $y)"
    }

}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
