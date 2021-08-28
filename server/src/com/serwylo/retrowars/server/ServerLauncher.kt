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

        val rooms = RetrowarsServer.Rooms.PublicRandomRooms(roomSize, maxRooms)

        val config = HeadlessApplicationConfiguration()
        HeadlessApplication(ServerApp(port, rooms, DesktopPlatform()), config)
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
