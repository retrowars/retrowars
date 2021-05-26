package com.serwylo.retrowars.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.serwylo.retrowars.core.*
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.scoring.Stats
import com.serwylo.retrowars.scoring.dumpStats
import com.serwylo.retrowars.scoring.loadAllStats
import com.serwylo.retrowars.scoring.saveHighScore
import java.util.*

class AnalyseStats : Game() {

    companion object {
        const val TAG = "Stats"
    }

    override fun create() {

        dumpStats()

        Gdx.app.exit()

    }

}
