package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.serwylo.retrowars.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.BetaInfo
import com.serwylo.retrowars.games.GameDetails

class BetaGameScreen(game: RetrowarsGame, gameDetails: GameDetails, betaInfo: BetaInfo): Scene2dScreen(game, { game.showGameSelectMenu() }) {

    init {
        val stage = this.stage
        val styles = game.uiAssets.getStyles()
        val strings = game.uiAssets.getStrings()

        val container = Table().apply {
            pad(UI_SPACE)
            setFillParent(true)

            row()
            add(
                makeHeading("Work in progress", styles, strings) {
                    game.showGameSelectMenu()
                }
            ).top()

            row()
            add(
                makeLargeButton("Play Game", styles) {
                    game.launchGame(gameDetails)
                }
            ).expandY().center()

            row().spaceTop(UI_SPACE * 2)
            add(
                Label("Thanks for trying this game while it is a work in progress.\nYour feedback can help improve it prior to release.", styles.label.medium).apply {
                    width = stage.width * 2f / 3f
                    setAlignment(Align.left)
                }
            ).left()

            if (betaInfo.description != null) {
                row().spaceTop(UI_SPACE * 2)
                add(
                    Label(betaInfo.description, styles.label.small).apply {
                        width = stage.width * 2f / 3f
                        setAlignment(Align.left)
                    }
                ).left()
            }

            row().spaceTop(UI_SPACE * 2)
            add(
                makeSmallButton("Provide Feedback via GitHub", styles) {
                    Gdx.net.openURI(betaInfo.feedbackUrl)
                }
            ).expandY().top()

        }

        stage.addActor(container)

        addToggleAudioButtonToMenuStage(game, stage)

    }

}
