package com.serwylo.retrowars.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.serwylo.retrowars.net.Network

object ServerLauncher {

    @JvmStatic
    fun main(arg: Array<String>) {

        val envPort = System.getenv("PORT")?.toIntOrNull()

        val cliPort = arg
            .find { it.startsWith("--port=") }
            ?.substring("--port=".length)
            ?.toIntOrNull()

        val port = cliPort ?: envPort ?: Network.defaultPort

        val config = HeadlessApplicationConfiguration()
        HeadlessApplication(ServerApp(port), config)
    }

}
