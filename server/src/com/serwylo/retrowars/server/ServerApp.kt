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
        Gdx.app.log(TAG, "Launching server app")

        server = RetrowarsServer(
            rooms = RetrowarsServer.Rooms.PublicRandomRooms(5),
            port = Network.defaultPort,
        )
    }

    override fun resize(width: Int, height: Int) {}
    override fun render() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}
}
