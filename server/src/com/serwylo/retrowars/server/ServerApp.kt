package com.serwylo.retrowars.server

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.OnlineServerDetails
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.net.ServerMetadataDTO
import io.javalin.Javalin
import io.javalin.http.Context
import java.net.InetAddress
import kotlin.math.max

class ServerApp: ApplicationListener {

    companion object {
        const val TAG = "ServerApp"
    }

    override fun create() {
        Gdx.app.log(TAG, "Creating server app")
        val app = Javalin.create().start(8080)
        app.get("/games") { ctx -> listGames(ctx) }
        app.get("/.well-known/com.serwylo.retrowars-servers.json") { ctx -> listServers(ctx) }

        RetrowarsServer(RetrowarsServer.Rooms.PublicRandomRooms(5), Network.defaultPort + 10, Network.defaultUdpPort + 10)
        RetrowarsServer(RetrowarsServer.Rooms.MultiplePrivateRooms(), Network.defaultPort + 12, Network.defaultUdpPort + 12)
    }

    private fun listGames(ctx: Context): Context {
        return ctx.json("")
    }

    private fun listServers(ctx: Context): Context {
        return ctx.json(listOf(
            ServerMetadataDTO(InetAddress.getLocalHost(), Network.defaultPort + 10, Network.defaultUdpPort + 10, ServerMetadataDTO.PUBLIC_RANDOM_ROOMS, 5, 10),
            ServerMetadataDTO(InetAddress.getLocalHost(), Network.defaultPort + 12, Network.defaultUdpPort + 12, ServerMetadataDTO.MULTIPLE_PRIVATE_ROOMS, 10, 10),
        ))
    }

    override fun resize(width: Int, height: Int) {}
    override fun render() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}
}
