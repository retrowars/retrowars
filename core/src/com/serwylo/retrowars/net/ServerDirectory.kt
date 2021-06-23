package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.IOException

private const val TAG = "ServerDirectory"

/**
 * @param httpPort Used to fetch statistics about the server.
 */
data class ServerMetadataDTO(val hostname: String, val httpPort: Int) {
    companion object {
        const val SINGLE_ROOM = "singleRoom"
        const val PUBLIC_RANDOM_ROOMS = "publicRandomRooms"
        const val MULTIPLE_PRIVATE_ROOMS = "multiplePrivateRooms"
    }
}

data class ServerInfoDTO(
    val tcpPort: Int,
    val udpPort: Int,
    val type: String,
    val maxPlayersPerRoom: Int,
    val maxRooms: Int,
    val currentRoomCount: Int,
    val currentPlayerCount: Int,
    val lastGameTimestamp: Long
)

/**
 * @param info If this is null, it means that we have been unable to contact the server.
 */
data class ServerDetails(
    val hostname: String,
    val httpPort: Int,

    val tcpPort: Int,
    val udpPort: Int,
    val type: String,
    val maxPlayersPerRoom: Int,
    val maxRooms: Int,
    val currentRoomCount: Int,
    val currentPlayerCount: Int,
    val lastGameTimestamp: Long
)

private val httpClient = HttpClient(CIO) {
    install(JsonFeature)
}

suspend fun fetchPublicServerList(): List<ServerMetadataDTO> {
    val url = "http://localhost:8080/.well-known/com.serwylo.retrowars-servers.json"

    return httpClient.get(url)
}

suspend fun fetchServerInfo(server: ServerMetadataDTO): ServerInfoDTO? {
    val url = URLBuilder(
        protocol = if (server.httpPort == 443) URLProtocol.HTTPS else URLProtocol.HTTP,
        port = server.httpPort,
        host = server.hostname,
        encodedPath = "info"
    ).build()

    try {
        return httpClient.get(url)
    } catch (e: IOException) {
        Gdx.app.error(TAG, "Could not fetch server metadata from $url", e)
        return null
    }
}