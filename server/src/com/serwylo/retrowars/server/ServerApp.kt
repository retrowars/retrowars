package com.serwylo.retrowars.server

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.net.*
import io.javalin.Javalin
import io.javalin.http.Context

class ServerApp: ApplicationListener {

    companion object {
        const val TAG = "ServerApp"
    }

    private lateinit var server: RetrowarsServer

    override fun create() {
        Gdx.app.log(TAG, "Creating server app")
        val app = Javalin.create().start(8080)
        app.get("/info") { ctx -> showStats(ctx) }
        app.get("/.well-known/com.serwylo.retrowars-servers.json") { ctx -> listServers(ctx) }

        server = RetrowarsServer(RetrowarsServer.Rooms.PublicRandomRooms(5), Network.defaultPort + 10, Network.defaultUdpPort + 10)
    }

    private fun showStats(ctx: Context): Context {
        return ctx.json(ServerInfoDTO(
            Network.defaultPort + 10,
            Network.defaultUdpPort + 10,
            ServerMetadataDTO.PUBLIC_RANDOM_ROOMS,
            5,
            10, // TODO: This isn't actually implemented yet.
            server.getRoomCount(),
            server.getPlayerCount(),
            server.getLastGameTime()?.time ?: 0
        ))
    }

    private fun listServers(ctx: Context): Context {
    return ctx.json(emptyList<ServerMetadataDTO>())
    /*return ctx.json(listOf(
            ServerMetadataDTO("localhost", 8080),
        ))*/
    }

    override fun resize(width: Int, height: Int) {}
    override fun render() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}
}
