package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.tetris.ButtonState
import com.serwylo.retrowars.input.TempestSoftController
import com.serwylo.retrowars.input.TetrisSoftController

class TempestGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.tempest,
    "Shoot the enemies",
    "Don't let them touch you",
    400f,
    400f
) {

    companion object {
        @Suppress("unused")
        const val TAG = "TempestGameScreen"
    }

    init {
        controller!!.listen(
            TempestSoftController.Buttons.MOVE_COUNTER_CLOCKWISE,
            { state.moveCounterClockwise = if (state.moveCounterClockwise == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held },
            { state.moveCounterClockwise = ButtonState.Unpressed })

        controller!!.listen(
            TempestSoftController.Buttons.MOVE_CLOCKWISE,
            { state.moveClockwise = if (state.moveClockwise == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held },
            { state.moveClockwise = ButtonState.Unpressed })
    }

    private val state = TempestGameState(viewport.worldWidth, viewport.worldHeight)

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
        recordInput()
        movePlayer()
        resetInput()
    }

    private fun recordInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            state.moveCounterClockwise = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            state.moveClockwise = ButtonState.JustPressed
        }
    }

    private fun resetInput() {
        state.moveCounterClockwise = ButtonState.Unpressed
        state.moveClockwise = ButtonState.Unpressed
    }

    private fun movePlayer() {
        val currentIndex = state.level.segments.indexOf(state.playerSegment)

        if (state.moveClockwise == ButtonState.JustPressed) {
            state.playerSegment = state.level.segments[(currentIndex + 1) % state.level.segments.size]
        } else if (state.moveCounterClockwise == ButtonState.JustPressed) {
            state.playerSegment = state.level.segments[(state.level.segments.size + currentIndex - 1) % state.level.segments.size]
        }
    }

    override fun onReceiveDamage(strength: Int) {
    }

    override fun renderGame(camera: OrthographicCamera) {
        renderLevel(camera)
    }

    private fun renderLevel(camera: OrthographicCamera) {
        val r = game.uiAssets.shapeRenderer

        r.begin(ShapeRenderer.ShapeType.Line)
        r.projectionMatrix = camera.combined

        state.level.segments.forEach { segment ->
            r.color = if (segment == state.playerSegment) Color.YELLOW else Color.BLUE
            r.line(segment.start, segment.end)
        }

        r.end()
    }

}
