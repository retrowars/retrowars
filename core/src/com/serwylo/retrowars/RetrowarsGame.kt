package com.serwylo.retrowars

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen

class RetrowarsGame : Game() {

    lateinit var assets: Assets

    override fun create() {
        assets = Assets()

        setScreen(AsteroidsGameScreen(this))

    }

    override fun dispose() {

    }

}