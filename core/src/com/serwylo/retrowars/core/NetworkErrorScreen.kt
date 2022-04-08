package com.serwylo.retrowars.core

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
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
            RetrowarsClient.get()?.listen({ _, _ -> })
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

            addActor(makeErrorInfo(code, message, styles, strings))

            addActor(
                makeButton(strings["btn.main-menu"], styles) {
                    game.showMainMenu()
                }
            )
        }

        stage.addActor(container)

    }

    private fun makeErrorInfo(code: Int, message: String, styles: UiAssets.Styles, strings: I18NBundle): Actor = when (code) {
        Network.ErrorCodes.NO_ROOMS_AVAILABLE -> showNoRoomsAvailable(styles, strings)
        Network.ErrorCodes.CLIENT_CLOSED_APP -> showClientClosedApp(styles, strings)
        Network.ErrorCodes.PLAYER_ID_IN_USE -> showPlayerIdInUse(styles, strings)
        Network.ErrorCodes.SERVER_SHUTDOWN -> showServerShutdown(styles, strings)
        else -> makeTitle(message, styles)
    }

    private fun makeReconnectButton(styles: UiAssets.Styles, label: String): Button? {
        val lastServer = RetrowarsClient.getLastServer() ?: return null
        return makeButton(label, styles) {
            game.showMultiplayerLobbyAndConnect(lastServer)
        }
    }

    private fun showNoRoomsAvailable(styles: UiAssets.Styles, strings: I18NBundle) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle(strings["network-error.server-full.title"], styles))
        addActor(makeContributeServerInfo(game.uiAssets))
        makeReconnectButton(styles, strings["network-error.btn.try-again"])?.also { reconnect ->
            addActor(reconnect)
        }
    }

    private fun showClientClosedApp(styles: UiAssets.Styles, strings: I18NBundle) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle(strings["network-error.remain-open.title"], styles))
        makeReconnectButton(styles, strings["network-error.btn.rejoin"])?.also { reconnect ->
            addActor(reconnect)
        }
    }

    private fun showPlayerIdInUse(styles: UiAssets.Styles, strings: I18NBundle) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle(strings["network-error.avatar-in-use.title"], styles))
        addActor(makeDetails(strings["network-error.avatar-in-use.details"], styles))
        makeReconnectButton(styles, strings["network-error.btn.try-again"])?.also { reconnect ->
            addActor(reconnect)
        }
    }

    private fun showServerShutdown(styles: UiAssets.Styles, strings: I18NBundle) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle(Replace with strings "The server has been shutdown", styles))
        addActor(makeDetails("This may be for scheduled maintenance, or it could have crashed.\nHopefully it will be back up again soon.\n\nPlease join another server to continue playing.", styles))
    }

    private fun makeTitle(errorMessage: String, styles: UiAssets.Styles) =
        Label(errorMessage, styles.label.medium).apply {
            setAlignment(Align.center)
        }

    private fun makeDetails(details: String, styles: UiAssets.Styles) =
        Label(details, styles.label.small).apply {
            setAlignment(Align.center)
        }

}
