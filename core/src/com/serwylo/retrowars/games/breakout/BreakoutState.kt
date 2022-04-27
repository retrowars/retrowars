package com.serwylo.retrowars.games.breakout

import com.badlogic.gdx.math.Vector2

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
        val rowY = worldHeight - ((NUM_BRICK_ROWS - row) * space) - ((NUM_BRICK_ROWS - row) * blockHeight)
        (0..NUM_BRICK_COLS).map { col ->
            Cell(col * blockWidth + col * space, rowY, true)
        }
    }

    val ballSpeed =  worldWidth / 2
    val ballPos = Vector2(worldWidth / 2, space * 2 + blockHeight)
    val ballPosPrevoius = ballPos.cpy()
    val ballVel = Vector2(0f, ballSpeed).rotateDeg(45f)

}

/**
 * @param x X-Position in world coordinates.
 * @param y Y-Position in world coordinates.
 */
data class Cell(
    val x: Float,
    val y: Float,
    var hasBlock: Boolean,
)