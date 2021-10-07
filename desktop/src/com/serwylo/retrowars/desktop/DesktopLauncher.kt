package com.serwylo.retrowars.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.utils.DesktopPlatform

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        if (arg.contains("--stats")) {

            val config = LwjglApplicationConfiguration()
            LwjglApplication(AnalyseStats(), config)

        } else {

            val verbose = arg.contains("--verbose")
            val randomAvatar = arg.contains("--force-random-avatar")

            val config = LwjglApplicationConfiguration()
            LwjglApplication(RetrowarsGame(DesktopPlatform(), verbose, randomAvatar), config)

        }
    }
}