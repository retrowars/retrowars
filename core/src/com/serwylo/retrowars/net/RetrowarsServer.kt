package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.net.Network.register
import java.lang.IllegalStateException
import kotlin.random.Random

class RetrowarsServer {

    companion object {

        private const val TAG = "RetrowarsServer"
        private var server: RetrowarsServer? = null

        fun start(): RetrowarsServer {
            if (server != null) {
                throw IllegalStateException("Cannot start a server, one has already been started.")
            }

            val newServer = RetrowarsServer()
            server = newServer
            return newServer
        }

        fun get(): RetrowarsServer? = server

        fun stop() {
            server?.close()
            server = null
        }

    }

    private var players = mutableSetOf<Player>()

    private var server = object : Server() {
        override fun newConnection() = PlayerConnection()
    }

    // This holds per connection state.
    internal class PlayerConnection: Connection() {
        var player: Player? = null
    }

    init {

        register(server)

        server.addListener(object : Listener {
            override fun received(c: Connection, obj: Any) {
                val connection = c as PlayerConnection
                if (obj !is FrameworkMessage.KeepAlive) {
                    Gdx.app.log(TAG, "Received message from client: $obj")
                }

                when (obj) {
                    is Network.Server.RegisterPlayer -> newPlayer(connection)
                    is Network.Server.UnregisterPlayer -> removePlayer(connection.player)
                    is Network.Server.UpdateScore -> updateScore(connection.player, obj.score)
                    is Network.Server.UpdateStatus -> updateStatus(connection.player, obj.status)
                }
            }

            override fun disconnected(c: Connection) {
                removePlayer((c as PlayerConnection).player)

            }
        })

        server.bind(Network.defaultPort)
        server.start()
    }

    private fun updateStatus(player: Player?, status: String) {
        if (player == null) {
            return
        }

        player.status = status

        // TODO: Don't send back to the client that originally reported their own death.
        server.sendToAllTCP(Network.Client.PlayerStatusChange(player.id, status))
    }

    private fun updateScore(player: Player?, score: Long) {
        if (player == null) {
            return
        }

        // TODO: Don't send back to the client that originally reported their own score.
        server.sendToAllTCP(Network.Client.PlayerScored(player.id, score))
    }

    private fun removePlayer(player: Player?) {
        if (player == null) {
            return
        }

        players.remove(player)
        server.sendToAllTCP(Network.Client.PlayerRemoved(player.id))
    }


    private fun newPlayer(connection: PlayerConnection) {
        // Ignore if already logged in.
        if (connection.player != null) {
            return
        }

        // TODO: Ensure this ID doesn't already exist on the server.
        val player = Player(Random.nextLong(), Games.asteroids.id /* Games.allSupported.random().id */)

        connection.player = player

        // First tell people about the new player (before sending a list of all existing players to
        // this newly registered client). That means that the first PlayerAdded message received by
        // a new client will always be for themselves.
        server.sendToAllTCP(Network.Client.PlayerAdded(player.id, player.game))

        // Then notify the current player about all others.
        players.forEach { existingPlayer ->
            connection.sendTCP(Network.Client.PlayerAdded(existingPlayer.id, existingPlayer.game))
        }

        players.add(player)
    }

    fun close() {
        server.close()
    }

    fun startGame() {
        server.sendToAllTCP(Network.Client.StartGame())
    }

}

