package com.serwylo.retrowars

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.serwylo.retrowars.core.MainMenuScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import java.util.*

class RetrowarsGame : Game() {

    lateinit var uiAssets: UiAssets

    override fun create() {
        uiAssets = UiAssets(Locale.getDefault())
        uiAssets.initSync()

        setScreen(MainMenuScreen(this))
    }

    fun showGameSelectMenu() {
        Gdx.app.postRunnable {
            setScreen(AsteroidsGameScreen(this))
        }
    }

    fun startGame(screen: Screen) {
        Gdx.app.postRunnable {
            setScreen(screen)
        }
    }

    override fun dispose() {

    }

}
