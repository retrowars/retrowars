package com.serwylo.retrowars

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.serwylo.retrowars.core.GameSelectScreen
import com.serwylo.retrowars.core.MainMenuScreen
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
            setScreen(GameSelectScreen(this))
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

}
