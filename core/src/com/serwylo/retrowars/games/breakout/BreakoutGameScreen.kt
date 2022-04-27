package com.serwylo.retrowars.games.breakout

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
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

    override fun updateGame(delta: Float) {

        state.targetX?.also { targetX ->
            if (state.paddleX < targetX) {
                state.paddleX = (state.paddleX + state.paddleSpeed * delta).coerceAtMost(targetX).coerceAtMost(viewport.worldWidth - state.paddleWidth / 2)
            } else {
                state.paddleX = (state.paddleX - state.paddleSpeed * delta).coerceAtLeast(targetX).coerceAtLeast(state.paddleWidth / 2)
            }
        }

        state.ballPosPrevoius.set(state.ballPos)
        state.ballPos.mulAdd(state.ballVel, delta)

        bounceOffWalls()
        breakBricks()

    }

    private fun breakBricks() {
        val pos = state.ballPos
        val size = state.blockHeight
        state.cells.forEach { row ->
            val y = row.first().y
            if (pos.y + size > y && pos.y < y + size) {
                row.forEach { cell ->
                    if (cell.hasBlock && pos.x + size > cell.x && pos.x < cell.x + state.blockWidth) {
                        cell.hasBlock = false

                        // TODO: Make this take the more dominant overlap rather than preferring the y axis.
                        //       Sometimes it flips downwards when it feels like it should have flipped horizontally instead.
                        val prev = state.ballPosPrevoius
                        val vel = state.ballVel
                        when {
                            prev.y + size < cell.y || prev.y > cell.y + size ->
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
    
    private fun bounceOffWalls() {
        val pos = state.ballPos
        val vel = state.ballVel
        val size = state.blockHeight

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
        } else if (vel.y < 0 && pos.y < state.space * 2 + size) {
            vel.y = -vel.y
            pos.y = state.space * 2 + size
        }
    }

    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined
        r.begin(ShapeRenderer.ShapeType.Filled)

        r.color = Color.WHITE
        r.rect(state.paddleX - state.paddleWidth / 2, state.blockHeight, state.paddleWidth, state.blockHeight)

        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                if (cell.hasBlock) {
                    r.rect(
                        cell.x,
                        cell.y,
                        state.blockWidth,
                        state.blockHeight,
                    )
                }
            }
        }

        r.rect(
            state.ballPos.x,
            state.ballPos.y,
            state.blockHeight,
            state.blockHeight,
        )

        r.end()
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