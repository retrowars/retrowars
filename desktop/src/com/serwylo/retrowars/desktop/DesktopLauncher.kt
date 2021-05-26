package com.serwylo.retrowars.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.net.RetrowarsServer

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        if (arg.contains("--server")) {

            println("Starting server...")
            val server = RetrowarsServer()
            while (true) {
                Thread.sleep(1000)
            }

        } else if (arg.contains("--stats")) {
            val config = LwjglApplicationConfiguration()
            LwjglApplication(AnalyseStats(), config)
        } else {
            val config = LwjglApplicationConfiguration()
            LwjglApplication(RetrowarsGame(), config)
        }
    }
}