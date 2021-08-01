package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games

class TempestGameScreen(game: RetrowarsGame) : GameScreen(game, Games.tempest, 400f, 400f) {

    companion object {
        @Suppress("unused")
        const val TAG = "TempestGameScreen"
    }

    private val state = TempestGameState()

    init {
        showMessage("Shoot the enemies", "Don't let them touch you")
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
    }

    override fun onReceiveDamage(strength: Int) {
    }

    override fun renderGame(camera: OrthographicCamera) {
    }

}
