package com.serwylo.retrowars.core

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
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
        Network.ErrorCodes.CLIENT_CLOSED_APP -> showClientClosedApp(styles)
        Network.ErrorCodes.PLAYER_ID_IN_USE -> showPlayerIdInUse(styles)
        Network.ErrorCodes.SERVER_SHUTDOWN -> showServerShutdown(styles)
        else -> makeTitle(message, styles)
    }

    private fun makeReconnectButton(styles: UiAssets.Styles, label: String): Button? {
        val lastServer = RetrowarsClient.getLastServer() ?: return null
        return makeButton(label, styles) {
            game.showMultiplayerLobbyAndConnect(lastServer)
        }
    }

    private fun showNoRoomsAvailable(styles: UiAssets.Styles) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle("Maximum number of players for this server has been reached.\nPlease try again later or join another server.", styles))
        addActor(makeContributeServerInfo(styles))
        val reconnect = makeReconnectButton(styles, "Try again")
        if (reconnect != null) {
            addActor(reconnect)
        }
    }

    private fun showClientClosedApp(styles: UiAssets.Styles) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle("Game must remain open while connected to the server.\nPlease rejoin to continue playing.", styles))
        val reconnect = makeReconnectButton(styles, "Rejoin")
        if (reconnect != null) {
            addActor(reconnect)
        }
    }

    private fun showPlayerIdInUse(styles: UiAssets.Styles) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle("Someone is already using your avatar... What are the odds?\nPlease either change your avatar, or wait until they leave.", styles))
        addActor(makeDetails("By the way: the odds of this happening randomly are 1 in 18446744073709551614...", styles))
        val reconnect = makeReconnectButton(styles, "Try again")
        if (reconnect != null) {
            addActor(reconnect)
        }
    }

    private fun showServerShutdown(styles: UiAssets.Styles) = VerticalGroup().apply {
        space(UI_SPACE * 2)
        addActor(makeTitle("The server has been shutdown", styles))
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
