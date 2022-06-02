package com.serwylo.retrowars.games.breakout

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.BreakoutSoftController
import java.util.*
import kotlin.math.abs

class BreakoutGameScreen(game: RetrowarsGame): GameScreen(
    game,
    Games.breakout,
    400f,
    250f,
    true
) {

    private val state = BreakoutState(viewport.worldWidth, viewport.worldHeight)

    private val lifeContainer = HorizontalGroup().apply { space(UI_SPACE) }

    init {
        addGameScoreToHUD(lifeContainer)
    }

    private fun redrawLives() {
        lifeContainer.clear()
        for (i in 0 until state.lives) {
            lifeContainer.addActor(Label("x", game.uiAssets.getStyles().label.large))
        }
    }

    override fun updateGame(delta: Float) {

        state.timer += delta

        controller!!.update(delta)

        if (getState() == State.Playing) {
            if (controller.noButtonsDescription == null) {
                state.isMovingLeft = controller.trigger(BreakoutSoftController.Buttons.LEFT)
                state.isMovingRight = controller.trigger(BreakoutSoftController.Buttons.RIGHT)
            }

            movePaddle(delta)
        }

        maybeRespawn()

        // Not waiting to respawn, move all the things.
        if (state.playerRespawnTime < 0) {
            moveBall(delta)

            bounceOffWalls()
            bounceOffPaddle()

            if (breakBricks()) {
                increaseScore(BreakoutState.SCORE_PER_BRICK)

                state.currentHandicapScore += BreakoutState.SCORE_PER_BRICK

                val factor = (state.currentHandicapScore.toFloat() / BreakoutState.MAX_HANDICAP_SCORE).coerceAtMost(1f)

                val minPaddleSize = state.initialPaddleWidth * BreakoutState.MAX_HANDICAP_PADDLE_SIZE_FACTOR
                val diffPaddleSize = state.initialPaddleWidth - minPaddleSize
                state.paddleWidth = state.initialPaddleWidth - (diffPaddleSize * factor)

                val maxBallSpeed = state.initialBallSpeed * BreakoutState.MAX_HANDICAP_BALL_SPEED_FACTOR
                val diffBallSpeed = maxBallSpeed - state.initialBallSpeed
                state.ballSpeed = state.initialBallSpeed + (diffBallSpeed * factor)

                if (state.cells.all { row -> row.all { cell -> !cell.hasBlock } }) {
                    restartLevel()
                }
            }
        }

        if (getState() == State.Playing && maybeDie()) {
            endGame()
        }

    }

    private fun restartLevel() {
        state.cells.forEach { row ->
            row.forEach { cell -> cell.hasBlock = true }
        }

        state.networkEnemyCells.clear()

        // Intentionally don't "resetPlay()" here, because the game is just a little too easy to make
        // last forever if you continually start each level fresh. If instead, you start with a little
        // pace on the ball and a smaller paddle, then it is more challenging while the blocks hang
        // nice and low, forcing you to move the paddle more to prevent impending doom.
        respawnSoon()
    }

    private fun maybeRespawn() {
        if (state.playerRespawnTime >= 0 && state.timer > state.playerRespawnTime) {

            state.ballPos.set(viewport.worldWidth / 2, state.paddleHeight + state.space + state.paddleHeight)
            state.ballPosPrevious.set(state.ballPos)
            state.ballVel.set(0f, state.ballSpeed).rotateDeg(45f)

            state.playerRespawnTime = -1f

        }
    }

    private fun maybeDie(): Boolean {
        return if (state.playerRespawnTime >= 0f || state.ballPos.y >= 0f) {

            false

        } else if (state.lives == 1) {

            state.lives = 0
            true

        } else {

            state.lives --
            resetHandicap()
            respawnSoon()
            false

        }
    }

    private fun resetHandicap() {
        state.paddleWidth = state.initialPaddleWidth
        state.ballSpeed = state.initialBallSpeed
        state.currentHandicapScore = 0
    }

    private fun respawnSoon() {
        state.playerRespawnTime = state.timer + BreakoutState.PAUSE_AFTER_DEATH
    }

    private fun movePaddle(delta: Float) {
        if (state.isMovingLeft) {
            if (state.paddleVelocity > 0f) {
                state.paddleVelocity = 0f
            }
            state.paddleVelocity = (state.paddleVelocity - delta * BreakoutState.PADDLE_ACCELERATION).coerceAtLeast(-state.maxPaddleSpeed * BreakoutState.SOFT_BUTTON_PADDLE_SPEED_FACTOR)
        }

        if (state.isMovingRight) {
            if (state.paddleVelocity < 0f) {
                state.paddleVelocity = 0f
            }
            state.paddleVelocity = (state.paddleVelocity + delta * BreakoutState.PADDLE_ACCELERATION).coerceAtMost(state.maxPaddleSpeed * BreakoutState.SOFT_BUTTON_PADDLE_SPEED_FACTOR)
        }

        if (state.paddleVelocity != 0f) {

            if (!state.isMovingLeft && !state.isMovingRight) {
                state.paddleVelocity = if (state.paddleVelocity < 0f) {
                    (state.paddleVelocity + delta * BreakoutState.PADDLE_ACCELERATION).coerceAtMost(0f)
                } else {
                    (state.paddleVelocity - delta * BreakoutState.PADDLE_ACCELERATION).coerceAtLeast(0f)
                }
            }

            state.paddleX += delta * state.paddleVelocity
        }

        if (controller?.noButtonsDescription != null) {
            state.targetX?.also { targetX ->
                if (state.paddleX < targetX) {
                    state.paddleX = (state.paddleX + state.maxPaddleSpeed * delta).coerceAtMost(targetX).coerceAtMost(viewport.worldWidth - state.paddleWidth / 2)
                } else {
                    state.paddleX = (state.paddleX - state.maxPaddleSpeed * delta).coerceAtLeast(targetX).coerceAtLeast(state.paddleWidth / 2)
                }
            }
        }

        state.paddleX = state.paddleX.coerceIn(state.paddleWidth / 2f, viewport.worldWidth - state.paddleWidth / 2f)
    }

    private fun moveBall(delta: Float) {
        state.ballPosPrevious.set(state.ballPos)
        state.ballPos.mulAdd(state.ballVel, delta)
    }

    private fun breakBricks(): Boolean {
        val pos = state.ballPos
        val size = state.ballSize
        state.cells.forEach { row ->
            val y = row.first().y
            if (pos.y + size > y && pos.y < y + size) {
                row.forEach { cell ->
                    if (cell.hasBlock && pos.x + size > cell.x && pos.x < cell.x + state.blockWidth) {
                        cell.hasBlock = false

                        // TODO: Make this take the more dominant overlap rather than preferring the y axis.
                        //       Sometimes it flips downwards when it feels like it should have flipped horizontally instead.
                        val prev = state.ballPosPrevious
                        val vel = state.ballVel
                        when {
                            prev.y + size < cell.y || prev.y > cell.y + state.blockHeight ->
                                vel.y = -vel.y

                            prev.x + state.blockWidth < cell.x || prev.x > cell.x + state.blockWidth ->
                                vel.x = -vel.x

                            else ->
                                // Don't expect to ever get here, but just in case of weirdness, make
                                // the ball head back down toward the paddle.
                                vel.y = - abs(vel.y)
                        }

                        return true
                    }
                }
            }
        }

        return false
    }

    private fun bounceOffPaddle() {
        if (state.ballPos.y > state.paddleY + state.paddleHeight || state.ballPos.y + state.paddleHeight < state.paddleY) {
            return
        }

        val left = state.paddleLeft()
        val right = state.paddleRight()

        if (state.ballPos.x > right || state.ballPos.x + state.ballSize < left) {
            return
        }

        when {
            // Coming in from the right, will bounce off in that direction and continue to fall to
            // the ground :(
            state.ballPosPrevious.x > right -> {
                state.ballVel.x = -state.ballVel.x
                state.ballPos.x = right
            }

            // As above, but for the left :(
            state.ballPosPrevious.x + state.ballSize < left -> {
                state.ballVel.x = -state.ballVel.x
                state.ballPos.x = left
            }

            else -> {
                val distanceFromCentre = state.paddleX - (state.ballPos.x + state.ballSize / 2)
                val percentageDistance = distanceFromCentre / (state.paddleWidth / 2)
                val angle = percentageDistance * BreakoutState.MAX_BALL_ANGLE_OFF_PADDLE

                state.ballVel.set(0f, state.ballSpeed).rotateDeg(angle)
                state.ballPos.y = state.paddleY + state.paddleHeight
            }
        }
    }
    
    private fun bounceOffWalls() {
        val pos = state.ballPos
        val vel = state.ballVel
        val size = state.ballSize

        if (vel.x > 0 && pos.x + size > viewport.worldWidth) {
            vel.x = -vel.x
            pos.x = viewport.worldWidth - size
        } else if (vel.x < 0 && pos.x < 0) {
            vel.x = -vel.x
            pos.x = 0f
        }

        if (vel.y > 0 && pos.y + size > viewport.worldHeight) {
            vel.y = -vel.y
            pos.y = viewport.worldHeight - size
        }
    }

    private val shadowColour = Color(0.5f, 0.5f, 0.5f, 0.15f)
    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined
        r.begin(ShapeRenderer.ShapeType.Filled)

        if (controller?.noButtonsDescription != null) {
            r.color = shadowColour
            r.rect(state.paddleLeft(), 0f, state.paddleWidth, viewport.worldHeight)
        }

        r.color = Color.WHITE
        r.rect(state.paddleLeft(), state.paddleY, state.paddleWidth, state.paddleHeight)

        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                if (cell.hasBlock) {
                    r.color = if (state.networkEnemyCells.contains(cell)) Color.RED else Color.WHITE
                    r.rect(cell.x, cell.y, state.blockWidth, state.blockHeight)
                }
            }
        }

        if (state.playerRespawnTime < 0) {
            r.color = Color.WHITE
            r.rect(state.ballPos.x, state.ballPos.y, state.ballSize, state.ballSize)
        }

        r.end()

        if (lifeContainer.children.size != state.lives) {
            redrawLives()
        }
    }

    override fun onReceiveDamage(strength: Int) {
        for (i in 0 until strength) {
            spawnNetworkEnemies()
        }
    }

    /**
     * Spawn randomly across all empty cells. Tried earlier just spawning in the bottom row first,
     * but resulted in a strange feeling. The randomness of any cell seems to make it feel more
     * organic and fun.
     *
     * TODO: If there are no eligible rows, then shall we consider spawning them closer to the player?
     */
    private fun spawnNetworkEnemies() {

        val cells = state.cells
            .fold(emptyList<Cell>()) { acc, row -> acc + row.filter { !it.hasBlock } }
            .toMutableList()

        for (j in 0 until 4) {

            if (cells.isEmpty()) {
                return
            }

            val index = Random().nextInt(cells.size)
            val cell = cells.removeAt(index)
            cell.hasBlock = true
            state.networkEnemyCells.add(cell)
        }

    }

    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(
            getInputProcessor(),
            object: InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    state.targetX = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat())).x
                    return true
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    state.targetX = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat())).x
                    return true
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    state.targetX = null
                    return true
                }
            }
        )
    }

}
