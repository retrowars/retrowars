package com.serwylo.retrowars.server

import com.badlogic.gdx.ApplicationListener
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.utils.Platform
import org.slf4j.LoggerFactory

class ServerApp(
    private val port: Int,
    private val rooms: RetrowarsServer.Rooms,
    private val platform: Platform,
): ApplicationListener {

    companion object {
        private val logger = LoggerFactory.getLogger(ServerApp::class.java)
    }

    private lateinit var server: RetrowarsServer

    override fun create() {
        logger.info("Launching server app on port $port.")

        server = RetrowarsServer(platform, rooms, port)
    }

    override fun resize(width: Int, height: Int) {}
    override fun render() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}
}
