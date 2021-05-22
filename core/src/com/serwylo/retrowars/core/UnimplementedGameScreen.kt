package com.serwylo.retrowars.core

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.beatgame.ui.makeStage
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails

class UnimplementedGameScreen(private val game: RetrowarsGame, private val gameDetails: GameDetails): ScreenAdapter() {

    private val stage = makeStage()

    init {
        val stage = this.stage
        val styles = game.uiAssets.getStyles()
        val strings = game.uiAssets.getStrings()

        val container = VerticalGroup().apply {
            setFillParent(true)
            align(Align.center)
            space(UI_SPACE)
        }

        container.addActor(
            makeHeading(strings["unimplemented-game.title"], styles, strings) {
                game.showGameSelectMenu()
            }
        )

        container.addActor(
            Label(strings["unimplemented-game.description"], styles.label.medium).apply {
                wrap = true
                setAlignment(Align.center)
                width = stage.width * 2f / 3f
            }
        )

        container.addActor(
            Label(strings["unimplemented-game.next-game-dev"], styles.label.small).apply {
                wrap = true
                setAlignment(Align.center)
                width = stage.width * 2f / 3f
            }
        )

        container.addActor(
            makeButton(
                strings.format("unimplemented-game.vote", strings[gameDetails.nameId]),
                styles
            ) {
                Gdx.net.openURI("https://github.com/retrowars/retrowars/labels/game-proposal")
            }
        )

            stage.addActor(container)

    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = InputMultiplexer(stage, object : InputAdapter() {

            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    game.showGameSelectMenu()
                    return true
                }

                return false
            }

        })
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

}
