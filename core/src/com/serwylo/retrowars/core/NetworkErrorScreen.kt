package com.serwylo.retrowars.core

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.ui.makeContributeServerInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NetworkErrorScreen(game: RetrowarsGame, code: Int, message: String): Scene2dScreen(game, { game.showMainMenu() }) {

    init {

        // Ensure that we don't have any lingering network connections around.
        GlobalScope.launch {
            RetrowarsClient.disconnect()
            RetrowarsServer.stop()
        }

        val styles = game.uiAssets.getStyles()
        val strings = game.uiAssets.getStrings()

        val container = VerticalGroup().apply {
            setFillParent(true)
            align(Align.center)
            space(UI_SPACE * 3)

            addActor(
                makeHeading(strings["network-error.title"], styles, strings)
            )

            addActor(makeErrorInfo(code, message, styles))

            addActor(
                makeButton(strings["btn.main-menu"], styles) {
                    game.showMainMenu()
                }
            )
        }

        stage.addActor(container)

    }

    private fun makeErrorInfo(code: Int, message: String, styles: UiAssets.Styles): Actor = when (code) {
        Network.ErrorCodes.NO_ROOMS_AVAILABLE -> showNoRoomsAvailable(styles)
        else -> makeErrorLabel(message, styles)
    }

    private fun showNoRoomsAvailable(styles: UiAssets.Styles) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeErrorLabel("Sorry, no rooms available. Please try again later.", styles))
        addActor(makeContributeServerInfo(styles))
    }

    private fun makeErrorLabel(errorMessage: String, styles: UiAssets.Styles) =
        Label(errorMessage, styles.label.medium).apply {
            setAlignment(Align.center)
        }

}
