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
import com.serwylo.retrowars.ui.makeContributeServerWidget
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
            space(UI_SPACE)
        }

        container.addActor(
            makeHeading(strings["network-error.title"], styles, strings)
        )

        container.addActor(
            Label(message, styles.label.medium).apply {
                setAlignment(Align.center)
            }
        )

        val extraContext = makeExtraContext(code, styles)
        if (extraContext != null) {
            container.addActor(extraContext)
        }

        container.addActor(
            makeButton(strings["btn.main-menu"], styles) {
                game.showMainMenu()
            }
        )

        stage.addActor(container)

    }

    private fun makeExtraContext(code: Int, styles: UiAssets.Styles): Actor? {
        return when (code) {
            Network.ErrorCodes.NO_ROOMS_AVAILABLE -> makeContributeServerWidget(styles)
            else -> null
        }
    }

}
