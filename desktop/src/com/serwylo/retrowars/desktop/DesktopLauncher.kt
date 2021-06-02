package com.serwylo.retrowars.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.net.RetrowarsServer

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        if (arg.contains("--stats")) {

            val config = LwjglApplicationConfiguration()
            LwjglApplication(AnalyseStats(), config)

        } else {

            val verbose = arg.contains("--verbose")

            val config = LwjglApplicationConfiguration()
            LwjglApplication(RetrowarsGame(verbose), config)

        }
    }
}