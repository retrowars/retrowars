package com.serwylo.retrowars.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.utils.DesktopPlatform

object ServerLauncher {

    @JvmStatic
    fun main(args: Array<String>) {

        val port = getIntArg("PORT", "port", args) ?: Network.defaultPort
        val maxRooms = getIntArg("MAX_ROOMS", "max-rooms", args) ?: 20
        val roomSize = getIntArg("ROOM_SIZE", "room-size", args) ?: 4
        val finalScoreDuration = getIntArg("FINAL_SCORE_DURATION", "final-score-duration", args) ?: 7500

        val serverConfig = RetrowarsServer.Config(
            rooms = RetrowarsServer.Rooms.PublicRandomRooms(roomSize, maxRooms),
            port,
            finalScoreDuration,
        )

        val gdxAppConfig = HeadlessApplicationConfiguration()

        HeadlessApplication(ServerApp(serverConfig, DesktopPlatform()), gdxAppConfig)

    }

    private fun getIntArg(envName: String, argName: String, args: Array<String>) =
        getStringArg(envName, argName, args)?.toIntOrNull()

    private fun getStringArg(envName: String, argName: String, args: Array<String>): String? {
        val envValue = System.getenv(envName)

        val cliValue = args
            .find { it.startsWith("--$argName=") }
            ?.substring("--$argName=".length)

        return cliValue ?: envValue
    }

}
