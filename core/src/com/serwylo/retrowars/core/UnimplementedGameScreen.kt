package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails

class UnimplementedGameScreen(game: RetrowarsGame, gameDetails: GameDetails): Scene2dScreen(game, { game.showGameSelectMenu() }) {

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
                strings.format("unimplemented-game.vote"),
                styles
            ) {
                Gdx.net.openURI("https://github.com/retrowars/retrowars/labels/game-proposal")
            }
        )

        stage.addActor(container)

    }

}
