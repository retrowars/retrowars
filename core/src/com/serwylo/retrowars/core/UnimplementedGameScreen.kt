package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.retrowars.ui.UI_SPACE
import com.serwylo.retrowars.ui.addToggleAudioButtonToMenuStage
import com.serwylo.retrowars.ui.makeButton
import com.serwylo.retrowars.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame

class UnimplementedGameScreen(game: RetrowarsGame): Scene2dScreen(game, { game.showGameSelectMenu() }) {

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

        addToggleAudioButtonToMenuStage(game, stage)

    }

}
