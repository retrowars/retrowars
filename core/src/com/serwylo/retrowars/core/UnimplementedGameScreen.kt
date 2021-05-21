package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
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
                game.showMainMenu()
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
