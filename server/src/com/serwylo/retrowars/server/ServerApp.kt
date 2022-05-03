package com.serwylo.retrowars.server

import com.badlogic.gdx.ApplicationListener
import com.serwylo.retrowars.net.RetrowarsServer
import com.serwylo.retrowars.utils.Platform
import org.slf4j.LoggerFactory

class ServerApp(
    private val config: RetrowarsServer.Config,
    private val platform: Platform,
): ApplicationListener {

    companion object {
        private val logger = LoggerFactory.getLogger(ServerApp::class.java)
    }

    private lateinit var server: RetrowarsServer

    override fun create() {
        logger.info("Launching server app [port: ${config.port}, rooms: [type: ${config.rooms.getName()}, maxRooms: ${config.rooms.getMaxRooms()}, roomSize: ${config.rooms.getRoomSize()}], finalScoreDelayMillis: ${config.finalScoreDelayMillis}, inactivePlayerTimeoutMillis: ${config.inactivePlayerTimeoutMillis}]")

        server = RetrowarsServer(platform, config)

        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down server app (will politely ask server to close all connections)")
            server.close()
        })
    }

    override fun resize(width: Int, height: Int) {}
    override fun render() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}

}
