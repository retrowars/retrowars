package com.serwylo.retrowars

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.core.*
import com.serwylo.retrowars.games.BetaInfo
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.net.ServerHostAndPort
import com.serwylo.retrowars.utils.Options
import com.serwylo.retrowars.utils.Platform
import java.util.*

class RetrowarsGame(val platform: Platform, private val verbose: Boolean, private val forceRandomAvatars: Boolean = false) : Game() {

    companion object {
        const val TAG = "RetrowarsGame"
    }

    lateinit var uiAssets: UiAssets

    override fun create() {
        if (verbose) {
            Gdx.app.logLevel = Application.LOG_DEBUG
        }

        if (forceRandomAvatars) {
            Options.forceRandomPlayerId()
        }

        uiAssets = UiAssets(UiAssets.getLocale())
        uiAssets.initSync()

        setScreen(MainMenuScreen(this))
    }

    fun showGameSelectMenu() {
        Gdx.app.log(TAG, "Showing game select screen")
        Gdx.app.postRunnable {
            setScreen(GameSelectScreen(this))
        }
    }

    fun showMultiplayerLobby() {
        Gdx.app.log(TAG, "Showing multiplayer lobby screen")
        Gdx.app.postRunnable {
            setScreen(MultiplayerLobbyScreen(this))
        }
    }

    fun showMultiplayerLobbyAndConnect(server: ServerHostAndPort) {
        Gdx.app.log(TAG, "Showing multiplayer lobby screen (in order to connect to $server)")
        Gdx.app.postRunnable {
            setScreen(MultiplayerLobbyScreen(this, server))
        }
    }

    fun showOptions() {
        Gdx.app.log(TAG, "Showing options screen")
        Gdx.app.postRunnable {
            setScreen(OptionsScreen(this))
        }
    }

    override fun dispose() {

    }

    fun showMainMenu() {
        Gdx.app.log(TAG, "Showing main menu screen")
        Gdx.app.postRunnable {
            setScreen(MainMenuScreen(this))
        }
    }

    fun showEndMultiplayerGame() {
        Gdx.app.log(TAG, "Showing end multiplayer game screen")
        Gdx.app.postRunnable {
            setScreen(MultiplayerLobbyScreen(this))
        }
    }

    fun showNetworkError(code: Int, message: String) {
        Gdx.app.log(TAG, "Showing network error screen")
        Gdx.app.postRunnable {
            setScreen(NetworkErrorScreen(this, code, message))
        }
    }

    fun launchGame(gameDetails: GameDetails) {
        Gdx.app.log(TAG, "Showing game screen for ${gameDetails.id}")
        Gdx.app.postRunnable {
            setScreen(gameDetails.createScreen(this))
        }
    }

    fun showBetaDetails(gameDetails: GameDetails, betaInfo: BetaInfo) {
        Gdx.app.log(TAG, "Showing beta info screen for ${gameDetails.id}")
        Gdx.app.postRunnable {
            setScreen(BetaGameScreen(this, gameDetails, betaInfo))
        }
    }

}
