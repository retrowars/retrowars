package com.serwylo.retrowars.net

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import java.net.InetAddress

data class ServerMetadataDTO(val hostname: InetAddress, val tcpPort: Int, val udpPort: Int, val type: String, val maxPlayersPerRoom: Int, val maxRooms: Int) {
    companion object {
        const val SINGLE_ROOM = "singleRoom"
        const val PUBLIC_RANDOM_ROOMS = "publicRandomRooms"
        const val MULTIPLE_PRIVATE_ROOMS = "multiplePrivateRooms"
    }
}

suspend fun fetchPublicServerList(): List<ServerMetadataDTO> {
    val url = "http://localhost:8080/.well-known/com.serwylo.retrowars-servers.json"

    val client = HttpClient(CIO) {
        install(JsonFeature)
    }

    val servers: List<ServerMetadataDTO> = client.get(url)

    return servers
}