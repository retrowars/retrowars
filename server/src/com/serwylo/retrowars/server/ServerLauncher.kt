package com.serwylo.retrowars.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.utils.DesktopPlatform
import kotlin.system.exitProcess

object ServerLauncher {

    val defaultMaxRooms = 20
    val defaultRoomSize = 4
    val defaultFinalScoreDurationMillis = 7500
    val defaultInactivePlayerTimeout = 90000

    @JvmStatic
    fun main(args: Array<String>) {

        val help = args.contains("--help") || args.contains("-h")
        if (help) {
            println("Usage: retrowars [--port=${Network.defaultPort}] [--max-rooms=${defaultMaxRooms}] [--room-size=${defaultRoomSize}] [--final-score-duration=${defaultFinalScoreDurationMillis}] [--inactive-player-timeout=${defaultInactivePlayerTimeout}] [--beta-games]\n" +
                    "       retrowars --help\n" +
                    "\n" +
                    "Each argument can also be configured via a corresponding environment variable, e.g. PORT / MAX_ROOMS / ROOM_SIZE, etc.")
            exitProcess(0)
        }

        val port = getIntArg("PORT", "port", args) ?: Network.defaultPort
        val maxRooms = getIntArg("MAX_ROOMS", "max-rooms", args) ?: defaultMaxRooms
        val roomSize = getIntArg("ROOM_SIZE", "room-size", args) ?: defaultRoomSize
        val finalScoreDurationMillis = getIntArg("FINAL_SCORE_DURATION", "final-score-duration", args) ?: defaultFinalScoreDurationMillis
        val inactivePlayerTimeoutMillis = getIntArg("INACTIVE_PLAYER_TIMEOUT", "inactive-player-timeout", args) ?: defaultInactivePlayerTimeout
        val betaGames = getBoolArg("BETA_GAMES", "--beta-games", args)

        val serverConfig = RetrowarsServer.Config(
            rooms = RetrowarsServer.Rooms.PublicRandomRooms(roomSize, maxRooms),
            port,
            finalScoreDurationMillis,
            inactivePlayerTimeoutMillis,
            includeBetaGames = betaGames,
        )

        val gdxAppConfig = HeadlessApplicationConfiguration()

        HeadlessApplication(ServerApp(serverConfig, DesktopPlatform()), gdxAppConfig)

    }

    private fun getBoolArg(envName: String, argName: String, args: Array<String>): Boolean {
        return System.getenv(envName) == "1" || args.contains(argName)
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
