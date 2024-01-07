package com.serwylo.retrowars.server

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.utils.DesktopPlatform
import kotlin.system.exitProcess

object ServerLauncher {

    val defaultMaxRooms = 20
    val defaultRoomSize = 4
    val defaultFinalScoreDurationMillis = 7500
    val defaultInactivePlayerTimeout = 90000

    fun usage() {
        println("Usage: retrowars [--port=${Network.defaultPort}] " +
                "[--max-rooms=${defaultMaxRooms}] " +
                "[--room-size=${defaultRoomSize}] " +
                "[--final-score-duration=${defaultFinalScoreDurationMillis}] " +
                "[--inactive-player-timeout=${defaultInactivePlayerTimeout}] " +
                "[--supported-games=${Games.allAvailable.map { it.id }.joinToString(",")}] " +
                "[--beta-games]\n" +
                "       retrowars --help\n" +
                "\n" +
                "Each argument can also be configured via a corresponding environment variable, e.g. PORT / MAX_ROOMS / ROOM_SIZE, etc.\n" +
                "\n" +
                "Note: --supported-games takes precedence over --beta. If you do not have --beta, but a beta game is included in the list of --supported-games, it will be available.")
    }

    @JvmStatic
    fun main(args: Array<String>) {

        val help = args.contains("--help") || args.contains("-h")
        if (help) {
            usage()
            exitProcess(0)
        }

        val port = getIntArg("PORT", "port", args) ?: Network.defaultPort
        val maxRooms = getIntArg("MAX_ROOMS", "max-rooms", args) ?: defaultMaxRooms
        val roomSize = getIntArg("ROOM_SIZE", "room-size", args) ?: defaultRoomSize
        val finalScoreDurationMillis = getIntArg("FINAL_SCORE_DURATION", "final-score-duration", args) ?: defaultFinalScoreDurationMillis
        val inactivePlayerTimeoutMillis = getIntArg("INACTIVE_PLAYER_TIMEOUT", "inactive-player-timeout", args) ?: defaultInactivePlayerTimeout
        val betaGames = getBoolArg("BETA_GAMES", "--beta-games", args)
        val supportedGameNames = getStringListArg("SUPPORTED_GAMES", "supported-games", args)

        val supportedGames = if (supportedGameNames.isNotEmpty()) {
            val gamesNotFound = supportedGameNames.filter { supportedGameName -> Games.allAvailable.none { g -> g.id == supportedGameName }}
            if (gamesNotFound.isNotEmpty()) {
                println("Could not find supported games: ${gamesNotFound.joinToString(", ")}")
                usage()
                exitProcess(1)
            }
            Games.allAvailable.filter { supportedGameNames.contains(it.id) }
        } else if (betaGames) {
            Games.allBeta
        } else {
            Games.allReleased
        }

        val serverConfig = RetrowarsServer.Config(
            rooms = RetrowarsServer.Rooms.PublicRandomRooms(roomSize, maxRooms),
            port,
            finalScoreDurationMillis,
            inactivePlayerTimeoutMillis,
            supportedGames,
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
    
    private fun getStringListArg(envName: String, argName: String, args: Array<String>): List<String> {
        return getStringArg(envName, argName, args)?.split(",") ?: emptyList();
    }

}
