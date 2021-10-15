package com.serwylo.retrowars.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.scoring.dumpStats

class AnalyseStats : Game() {

    companion object {
        const val TAG = "Stats"
    }

    override fun create() {

        dumpStats()

        Gdx.app.exit()

    }

}
