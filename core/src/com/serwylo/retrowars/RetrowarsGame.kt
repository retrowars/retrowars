package com.serwylo.retrowars

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.serwylo.retrowars.core.*
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.scoring.saveHighScore
import com.serwylo.retrowars.utils.Platform
import java.util.*

class RetrowarsGame(val platform: Platform, private val verbose: Boolean) : Game() {

    companion object {
        const val TAG = "RetrowarsGame"
    }

    lateinit var uiAssets: UiAssets

    override fun create() {
        if (verbose) {
            Gdx.app.logLevel = Application.LOG_DEBUG
        }

        uiAssets = UiAssets(Locale.getDefault())
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
            setScreen(EndMultiplayerGameScreen(this))
        }
    }

    fun showNetworkError(game: RetrowarsGame, wasGraceful: Boolean) {
        Gdx.app.log(TAG, "Showing network error screen")
        Gdx.app.postRunnable {
            setScreen(NetworkErrorScreen(this, wasGraceful))
        }
    }

    fun launchGame(gameDetails: GameDetails) {
        Gdx.app.log(TAG, "Showing game screen for ${gameDetails.id}")
        Gdx.app.postRunnable {
            setScreen(gameDetails.createScreen(this))
        }
    }

}
