package com.serwylo.retrowars.games.breakout

class BreakoutState(worldWidth: Float, worldHeight: Float) {

    companion object {
        const val NUM_BRICK_ROWS = 5
        const val NUM_BRICK_COLS = 12
    }

    var targetX: Float? = null
    var paddleX: Float = worldWidth / 2f

    val blockWidth = worldWidth / (NUM_BRICK_COLS + 1)
    val blockHeight = blockWidth / 4f
    val space = (worldWidth - (blockWidth * NUM_BRICK_COLS)) / (NUM_BRICK_COLS + 1)

    var paddleWidth = blockWidth * 2f
    val paddleSpeed = worldWidth

    val cells = (0..NUM_BRICK_ROWS).map { row ->
        (0..NUM_BRICK_COLS).map { col ->
            Cell(col, row, true)
        }
    }

}

data class Cell(
    val x: Int,
    val y: Int,
    val hasBlock: Boolean,
)