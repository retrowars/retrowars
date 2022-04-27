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

        if (getState() == State.Playing) {
            movePaddle(delta)
        }

        maybeRespawn()

        moveBall(delta)

        bounceOffWalls()
        bounceOffPaddle()

        breakBricks()

        if (getState() == State.Playing && maybeDie()) {
            endGame()
        }

    }

    private fun maybeRespawn() {
        if (state.playerRespawnTime >= 0 && state.timer > state.playerRespawnTime) {

            state.ballPos.set(viewport.worldWidth / 2, state.space * 2 + state.paddleHeight)
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
            state.playerRespawnTime = state.timer + BreakoutState.PAUSE_AFTER_DEATH
            false

        }
    }

    private fun movePaddle(delta: Float) {
        state.targetX?.also { targetX ->
            if (state.paddleX < targetX) {
                state.paddleX = (state.paddleX + state.paddleSpeed * delta).coerceAtMost(targetX).coerceAtMost(viewport.worldWidth - state.paddleWidth / 2)
            } else {
                state.paddleX = (state.paddleX - state.paddleSpeed * delta).coerceAtLeast(targetX).coerceAtLeast(state.paddleWidth / 2)
            }
        }
    }

    private fun moveBall(delta: Float) {
        state.ballPosPrevious.set(state.ballPos)
        state.ballPos.mulAdd(state.ballVel, delta)
    }

    private fun breakBricks() {
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
                    }
                }
            }
        }
    }

    private fun bounceOffPaddle() {
        if (state.ballPos.y > state.space + state.paddleHeight || state.ballPos.y + state.paddleHeight < state.space) {
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
                state.ballVel.y = -state.ballVel.y
                state.ballPos.y = state.space + state.ballSize
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

    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined
        r.begin(ShapeRenderer.ShapeType.Filled)

        r.color = Color.WHITE
        r.rect(state.paddleLeft(), state.space, state.paddleWidth, state.paddleHeight)

        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                if (cell.hasBlock) {
                    r.rect(cell.x, cell.y, state.blockWidth, state.blockHeight)
                }
            }
        }

        r.rect(state.ballPos.x, state.ballPos.y, state.ballSize, state.ballSize)

        r.end()

        if (lifeContainer.children.size != state.lives) {
            redrawLives()
        }
    }

    override fun onReceiveDamage(strength: Int) {

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