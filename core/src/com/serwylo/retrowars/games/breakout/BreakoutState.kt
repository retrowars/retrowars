package com.serwylo.retrowars.games.breakout

import com.badlogic.gdx.math.Vector2

class BreakoutState(worldWidth: Float, worldHeight: Float) {

    companion object {
        const val NUM_BRICK_ROWS = 5
        const val NUM_BRICK_COLS = 12
        const val PAUSE_AFTER_DEATH = 2f
        const val MAX_BALL_ANGLE_OFF_PADDLE = 60f // where "0" means directly up, and "90" means directly to the right.
    }

    var targetX: Float? = null
    var paddleX: Float = worldWidth / 2f

    val blockWidth = worldWidth / (NUM_BRICK_COLS + 1)
    val blockHeight = blockWidth / 4f
    val ballSize = blockHeight
    val space = (worldWidth - (blockWidth * NUM_BRICK_COLS)) / (NUM_BRICK_COLS + 1)

    var paddleWidth = blockWidth * 2f
    val paddleHeight = blockHeight
    val paddleSpeed = worldWidth

    val cells = (0..NUM_BRICK_ROWS).map { row ->
        val rowY = worldHeight - ((NUM_BRICK_ROWS - row) * space) - ((NUM_BRICK_ROWS - row) * blockHeight)
        (0..NUM_BRICK_COLS).map { col ->
            Cell(col * blockWidth + col * space, rowY, true)
        }
    }

    val ballSpeed =  worldWidth / 2
    val ballPos = Vector2(worldWidth / 2, space * 2 + blockHeight)
    val ballPosPrevious: Vector2 = ballPos.cpy()
    val ballVel: Vector2 = Vector2(0f, ballSpeed).rotateDeg(45f)

    fun paddleLeft(): Float = paddleX - paddleWidth / 2
    fun paddleRight(): Float = paddleX + paddleWidth / 2

    var lives = 3

    var timer: Float = 0f
    var playerRespawnTime: Float = -1f

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