package com.serwylo.retrowars

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.serwylo.retrowars.core.*
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.scoring.saveHighScore
import java.util.*

class RetrowarsGame : Game() {

    companion object {
        const val TAG = "RetrowarsGame"
    }

    lateinit var uiAssets: UiAssets

    override fun create() {
        uiAssets = UiAssets(Locale.getDefault())
        uiAssets.initSync()

        setScreen(MainMenuScreen(this))
    }

    fun showGameSelectMenu() {
        Gdx.app.postRunnable {
            setScreen(GameSelectScreen(this))
        }
    }

    fun showMultiplayerLobby() {
        Gdx.app.postRunnable {
            setScreen(MultiplayerLobbyScreen(this))
        }
    }

    fun showOptions() {
        Gdx.app.postRunnable {
            setScreen(OptionsScreen(this))
        }
    }

    fun startGame(screen: Screen) {
        Gdx.app.postRunnable {
            setScreen(screen)
        }
    }

    override fun dispose() {

    }

    fun showMainMenu() {
        Gdx.app.postRunnable {
            setScreen(MainMenuScreen(this))
        }
    }

    fun showEndMultiplayerGame() {
        Gdx.app.postRunnable {
            setScreen(EndMultiplayerGameScreen(this))
        }
    }

    fun showNetworkError(game: RetrowarsGame, wasGraceful: Boolean) {
        Gdx.app.postRunnable {
            setScreen(NetworkErrorScreen(this, wasGraceful))
        }
    }

}
