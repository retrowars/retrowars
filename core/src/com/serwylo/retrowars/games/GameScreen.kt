package com.serwylo.retrowars.games

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.ui.GameViewport
import com.serwylo.retrowars.ui.HUD

abstract class GameScreen(protected val game: RetrowarsGame, minWorldWidth: Float, maxWorldWidth: Float) : Screen {

    private val camera = OrthographicCamera()
    protected val viewport = GameViewport(minWorldWidth, maxWorldWidth, camera)

    private val hud: HUD

    protected val client = RetrowarsClient.get()

    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        viewport.apply(true)
        hud = HUD(game.uiAssets)
    }

    protected abstract fun getScore(): Long

    protected abstract fun updateGame(delta: Float)
    protected abstract fun renderGame(camera: OrthographicCamera)

    override fun render(delta: Float) {

        updateGame(delta)

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        Gdx.graphics.gL20.glClearColor(0f, 0f, 0f, 1f)
        Gdx.graphics.gL20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.renderIn {
            renderGame(camera)
        }

        hud.render(getScore(), delta)

        Gdx.gl.glDisable(GL20.GL_BLEND)

    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        hud.resize(width, height)

        resizeViewport(viewport.worldWidth, viewport.worldHeight)
    }

    open fun resizeViewport(viewportWidth: Float, viewportHeight: Float) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
    }
}
