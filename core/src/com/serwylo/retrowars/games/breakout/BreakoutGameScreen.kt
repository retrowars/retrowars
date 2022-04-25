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
                        ((x + 1) * state.space) + (x * state.blockWidth),
                        viewport.worldHeight - (((y + 1) * state.space) + ((y + 1) * state.blockHeight)),
                        state.blockWidth,
                        state.blockHeight,
                    )
                }
            }
        }

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