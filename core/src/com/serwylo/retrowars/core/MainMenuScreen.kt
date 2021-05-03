package com.serwylo.retrowars.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame

class MainMenuScreen(private val game: RetrowarsGame): ScreenAdapter() {

    private val stage = makeStage()

    init {
        val styles = game.uiAssets.getStyles()
        val strings = game.uiAssets.getStrings()

        val container = VerticalGroup().apply {
            setFillParent(true)
            align(Align.center)
            space(UI_SPACE)
        }

        container.addActor(
            makeHeading(strings["app.name"], styles, strings)
        )

        container.addActor(
            makeLargeButton(strings["main-menu.btn.play-single-player"], styles) {
                game.showGameSelectMenu()
            }
        )

        container.addActor(
            makeLargeButton(strings["main-menu.btn.play-multiplayer"], styles) {
                game.showMultiplayerLobby()
            }
        )


        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            container.addActor(
                makeButton(strings["main-menu.btn.quit"], styles) {
                    Gdx.app.exit()
                }
            )
        }

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
