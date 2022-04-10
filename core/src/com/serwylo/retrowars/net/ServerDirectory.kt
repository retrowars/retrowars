
package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.google.gson.annotations.Since
import com.serwylo.retrowars.utils.AppProperties
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.IOException
import kotlin.system.measureTimeMillis

private const val TAG = "ServerDirectory"

/**
 * This is the data known by the global directory of servers.
 * We ping the global directory once to get a list of well known public servers, and with this
 * information we can then request more specific information from each server by asking for:
 * http(s)://hostname/.well-known/com.serwylo.retrowars-servers.json which will in turn contain
 * a [ServerInfoDTO] object containing more information about the server.
 * @param port Used to fetch statistics about the server.
 *
 * Version history:
 *  - 9 (v0.7.0) Initial support for online multiplayer.
 */
data class ServerMetadataDTO(

    @Since(9.0)
    val hostname: String,

    @Since(9.0)
    val port: Int,

)

/**
 * Realtime information about a well-known public server. Can be used to infer whether this
 * server is currently being used by many people. If we know the answer to that, then users can
 * stop wasting time sitting in the lobby for servers that rarely get used.
 *
 * Version history:
 *  - 9 (v0.7.0) Initial support for online multiplayer.
 */
data class ServerInfoDTO(

    @Since(9.0)
    val versionCode: Int,

    @Since(9.0)
    val versionName: String,

    @Since(9.0)
    val minSupportedClientVersionCode: Int,

    @Since(9.0)
    val minSupportedClientVersionName: String,

    @Since(9.0)
    val type: String,

    @Since(9.0)
    val maxPlayersPerRoom: Int,

    @Since(9.0)
    val maxRooms: Int,

    @Since(9.0)
    val currentRoomCount: Int,

    @Since(9.0)
    val currentPlayerCount: Int,

    @Since(9.0)
    val lastGameTimestamp: Long,

    @Since(37.0)
    val lastPlayerTimestamp: Long = LAST_PLAYER_TIMESTAMP_NEVER,

) {
    companion object {
        const val LAST_PLAYER_TIMESTAMP_NEVER = -1L
    }
}

/**
 * After fetching the hostname and port from the global server directory, and then using that to
 * fetch more specific information about the server, these two things are combined into a [ServerDetails]
 * object. In addition, it will include [ServerDetails.pingTime] which is an approximation of how
 * long it takes to reach the server.
 */
data class ServerDetails(
    val hostname: String,
    val port: Int,
    val versionCode: Int,
    val versionName: String,
    val minSupportedClientVersionCode: Int,
    val minSupportedClientVersionName: String,
    val type: String,
    val maxPlayersPerRoom: Int,
    val maxRooms: Int,
    val currentRoomCount: Int,
    val currentPlayerCount: Int,
    val lastGameTimestamp: Long,
    val lastPlayerTimestamp: Long,

    /**
     * Approx ping time - the time it took to ask the server for its details.
     */
    val pingTime: Int,
)

private val httpClient = HttpClient(CIO) {

    install(JsonFeature) {
        serializer = GsonSerializer {
            setVersion(AppProperties.appVersionCode.toDouble())
        }
    }

    install(HttpTimeout) {
        // These timeouts are much higher than what would normally offer a good user experience.
        // The reason for this is because some of the public servers are running on free Heroku Dyno's
        // which go to sleep after 30mins of inactivity, and can take up to 20 seconds to restart.
        // A 30 second timeout should be enough to have the Dyno startup and the server to start.
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 30000
    }

}

suspend fun fetchPublicServerList(): List<ServerMetadataDTO> {
    val url = "https://retrowars.github.io/retrowars-servers/.well-known/com.serwylo.retrowars-servers.json"
    return httpClient.get(url)
}

suspend fun fetchServerInfo(server: ServerMetadataDTO): ServerInfoDTO? {
    val url = URLBuilder(
        protocol = if (server.port == 443) URLProtocol.HTTPS else URLProtocol.HTTP,
        port = server.port,
        host = server.hostname,
        encodedPath = "info"
    ).build()

        val data: ServerInfoDTO?
        val time = measureTimeMillis {
            Gdx.app.log(TAG, "Fetching data for $url")
            data = httpClient.get(url)
        }
        Gdx.app.log(TAG, "Fetched data for $url in ${time}ms.")
        return data
}