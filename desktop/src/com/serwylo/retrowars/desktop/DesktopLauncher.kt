package com.serwylo.retrowars.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.utils.DesktopPlatform
import java.util.Locale

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        if (arg.contains("--stats")) {

            val config = LwjglApplicationConfiguration()
            LwjglApplication(AnalyseStats(), config)

        } else {

            val verbose = arg.contains("--verbose")
            val randomAvatar = arg.contains("--force-random-avatar")
            val lang = arg.find { it.startsWith("--lang=") }?.substringAfter("=")
            val locale = if (lang == null) null else {
                val parts = lang.split(Regex("[-_]"))
                if (parts.size > 1) {
                    Locale(parts[0], parts[1])
                } else {
                    Locale(parts[0])
                }
            }

            val config = LwjglApplicationConfiguration()
            config.title = "Super Retro Mega Wars ${arg.joinToString(" ")}"
            LwjglApplication(RetrowarsGame(DesktopPlatform(), verbose, randomAvatar, locale), config)

        }
    }
}