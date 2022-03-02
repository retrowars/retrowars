package com.serwylo.retrowars.games.spaceinvaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.SpaceInvadersSoftController

class SpaceInvadersGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.spaceInvaders,
    "Shoot the aliens",
    "Dodge their shots",
    400f,
    400f,
    true
) {

    private val state = SpaceInvadersGameState(viewport.worldWidth, viewport.worldHeight)

    override fun updateGame(delta: Float) {
        state.timer += delta

        controller!!.update(delta)

        if (getState() == State.Playing) {
            state.isMovingLeft = controller.trigger(SpaceInvadersSoftController.Buttons.LEFT)
            state.isMovingRight = controller.trigger(SpaceInvadersSoftController.Buttons.RIGHT)
            state.isFiring = controller.trigger(SpaceInvadersSoftController.Buttons.FIRE)
        }

        updateEntities(delta)
    }

    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        r.begin(ShapeRenderer.ShapeType.Filled)
        r.color = Color.WHITE
        r.rect(
            state.playerX - state.cellWidth / 2,
            state.padding,
            state.cellWidth,
            state.cellHeight,
        )
        r.end()
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun onReceiveDamage(strength: Int) {

    }

    private fun updateEntities(delta: Float) {

    }

}