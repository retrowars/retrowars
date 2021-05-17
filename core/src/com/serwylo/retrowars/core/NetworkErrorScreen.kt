package com.serwylo.retrowars.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.net.RetrowarsServer

class NetworkErrorScreen(private val game: RetrowarsGame, wasGraceful: Boolean): ScreenAdapter() {

    private val stage = makeStage()

    init {
        // Ensure that we don't have any lingering network connections around.
        RetrowarsClient.disconnect()
        RetrowarsServer.stop()

        val styles = game.uiAssets.getStyles()
        val strings = game.uiAssets.getStrings()

        val container = VerticalGroup().apply {
            setFillParent(true)
            align(Align.center)
            space(UI_SPACE)
        }

        container.addActor(
            makeHeading(strings["network-error.title"], styles, strings)
        )

        container.addActor(
            Label(
                if (wasGraceful) strings["network-error.disconnected.server-stopped"] else strings["network-error.disconnected.unexpected"],
                styles.label.medium
            ).apply {
                setAlignment(Align.center)
            }
        )

        container.addActor(
            makeButton(strings["btn.main-menu"], styles) {
                game.showMainMenu()
            }
        )

        stage.addActor(container)

    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

}
