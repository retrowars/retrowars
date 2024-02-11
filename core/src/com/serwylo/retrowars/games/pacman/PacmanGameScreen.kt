package com.serwylo.retrowars.games.pacman

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.PacmanSoftController
import com.serwylo.retrowars.ui.ENEMY_ATTACK_COLOUR

class PacmanGameScreen(game: RetrowarsGame) : GameScreen(game, Games.pacman, 400f, 400f) {

    companion object {
        @Suppress("unused")
        const val TAG = "PacmanGameScreen"
    }

    private val state = PacmanGameState()
    private val sounds = PacmanSoundLibrary()
    override fun getSoundLibrary() = sounds

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
        if (getState() != State.Playing) {
            // There are no items to continue animating at the end of the game, so don't bother updating.
            return
        }

        state.timer += delta

        controller!!.update(delta)
        decideNextDirection()
        movePacman()

    }

    /**
     * Based on the current keys pressed and the current direction, queue up the next direction to
     * travel. This will be applied the next time the snake needs to inch forward. If you press one
     * direction then another very fast, you can queue up the next direction many times before the
     * snake actually moves.
      */
    private fun decideNextDirection() {
        val left = controller!!.trigger(PacmanSoftController.Buttons.LEFT)
        val right = controller.trigger(PacmanSoftController.Buttons.RIGHT)
        val up = controller.trigger(PacmanSoftController.Buttons.UP)
        val down = controller.trigger(PacmanSoftController.Buttons.DOWN)

        if (left && state.currentDirection != Direction.RIGHT && !right && !up && !down) {
            state.nextDirection = Direction.LEFT
        } else if (right && state.currentDirection != Direction.LEFT && !left && !up && !down) {
            state.nextDirection = Direction.RIGHT
        } else if (up && state.currentDirection != Direction.DOWN && !left && !right && !down) {
            state.nextDirection = Direction.UP
        } else if (down && state.currentDirection != Direction.UP && !left && !right && !up) {
            state.nextDirection = Direction.DOWN
        }
    }

    private fun movePacman() {
        if (state.timer < state.nextTimeStep) {
            return
        }

        state.nextTimeStep = state.nextTimeStep + state.timeStep
        state.currentDirection = state.nextDirection

    }

    private fun moveTo(direction: Direction, current: PacmanGameState.Cell) = null
        /*
        when(direction) {
            Direction.UP ->
            Direction.DOWN ->
            Direction.LEFT ->
            Direction.RIGHT ->
        }
        */

    override fun onReceiveDamage(strength: Int) {
    }

    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        r.begin(ShapeRenderer.ShapeType.Filled)

        r.color = Color.BLUE
        // r.rect(state.food.x * cellWidth + 1, state.food.y * cellHeight + 1, cellWidth - 2, cellHeight - 2)

        r.end()
    }

}
