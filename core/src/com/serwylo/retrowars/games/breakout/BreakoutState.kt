package com.serwylo.retrowars.games.breakout

import com.badlogic.gdx.math.Vector2

class BreakoutState(worldWidth: Float, worldHeight: Float) {

    companion object {
        /**
         * Drop the first row this many rows from the top of the screen to allow the ball to bounce
         * around the top of the screen once a player breaks through: https://youtu.be/Cr6z3AyhRr8?t=114
         */
        const val FIRST_ROW_OFFSET = 4
        const val NUM_BRICK_ROWS = 6
        const val NUM_BRICK_COLS = 12
        const val PAUSE_AFTER_DEATH = 1f
        const val MAX_BALL_ANGLE_OFF_PADDLE = 60f // where "0" means directly up, and "90" means directly to the right.
        const val SCORE_PER_BRICK = 3000

        /**
         * After starting a new life, accumulating this much score will result in:
         *  - The fastest ball movement, and
         *  - The smallest paddle
         */
        const val MAX_HANDICAP_SCORE = SCORE_PER_BRICK * 20
        const val MAX_HANDICAP_PADDLE_SIZE_FACTOR = 0.55f
        const val MAX_HANDICAP_BALL_SPEED_FACTOR = 1.3f

        /**
         * When using soft controllers, the paddle moves a little slower to make it more manageable.
         * When dragging a finger, it is easier to move very quickly to the final destination, because
         * your finger is literally there. However when using soft buttons, you kind of have to guess
         * when your paddle is in the right spot and hope you release at the right time. Slowing down
         * the movement helps to deal with this.
         */
        const val SOFT_BUTTON_PADDLE_SPEED_FACTOR = 0.8f

        /**
         * This is measured as a factor of the [maxPaddleSpeed] (which in turn is determined by the
         * viewport width).
         */
        const val PADDLE_ACCELERATION = 2000f
    }

    var targetX: Float? = null
    var isMovingLeft = false
    var isMovingRight = false
    var paddleX: Float = worldWidth / 2f

    val blockWidth = worldWidth / (NUM_BRICK_COLS + 0.75f)
    val blockHeight = worldHeight / 30f
    val ballSize = blockHeight
    val space = ((worldWidth - (blockWidth * NUM_BRICK_COLS)) / (NUM_BRICK_COLS + 0.75f))

    var initialPaddleWidth = blockWidth * 2f
    var paddleWidth = blockWidth * 2f
    val paddleHeight = blockHeight
    val paddleY = space * 10
    val maxPaddleSpeed = worldWidth
    var paddleVelocity = 0f

    val cells = (0 until NUM_BRICK_ROWS).map { row ->
        val rowY = worldHeight - ((NUM_BRICK_ROWS - row) * space) - ((FIRST_ROW_OFFSET + NUM_BRICK_ROWS - row) * blockHeight)
        (0 until NUM_BRICK_COLS).map { col ->
            Cell(space + col * blockWidth + col * space, rowY, true)
        }
    }

    val networkEnemyCells = mutableSetOf<Cell>()

    val initialBallSpeed = worldHeight * 0.75f
    var ballSpeed =  initialBallSpeed
    val ballPos = Vector2(worldWidth / 2, paddleY + paddleHeight + space)
    val ballPosPrevious: Vector2 = ballPos.cpy()
    val ballVel: Vector2 = Vector2(0f, ballSpeed).rotateDeg(45f)

    var currentHandicapScore = 0

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