package com.serwylo.retrowars.server

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.OnlineServerDetails
import com.serwylo.retrowars.net.RetrowarsServer
import io.javalin.Javalin
import io.javalin.http.Context
import kotlin.math.max

class ServerApp: ApplicationListener {

    companion object {
        const val TAG = "ServerApp"
    }

    override fun create() {
        Gdx.app.log(TAG, "Creating server app")
        val app = Javalin.create().start(8080)
        app.get("/games") { ctx -> listGames(ctx) }

        RetrowarsServer(RetrowarsServer.Rooms.PublicRandomRooms(2))
    }

    private fun listGames(ctx: Context): Context {
        return ctx.json("")
    }

    override fun resize(width: Int, height: Int) {}
    override fun render() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}
}
