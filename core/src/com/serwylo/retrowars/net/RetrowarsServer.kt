package com.serwylo.retrowars.net

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.serwylo.retrowars.net.Network.register
import kotlin.random.Random

class RetrowarsServer {

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

        server.addListener(object : Listener() {
            override fun received(c: Connection, obj: Any) {
                val connection = c as PlayerConnection
                println("Received message: $obj")
                when (obj) {
                    is Network.Server.RegisterPlayer -> newPlayer(connection)
                    is Network.Server.UnregisterPlayer -> removePlayer(connection.player)
                    is Network.Server.UpdateScore -> updateScore(connection.player, obj.score)
                }
            }

            override fun disconnected(c: Connection) {
                removePlayer((c as PlayerConnection).player)

            }
        })

        server.bind(Network.defaultPort)
        server.start()
    }

    private fun updateScore(player: Player?, score: Int) {
        if (player == null) {
            return
        }

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
        val player = Player(Random.nextLong())

        connection.player = player

        // First tell people about the new player (before sending a list of all existing players to
        // this newly registered client). That means that the first PlayerAdded message received by
        // a new client will always be for themselves.
        server.sendToAllTCP(Network.Client.PlayerAdded(player.id))

        // Then notify the current player about all others.
        players.forEach { existingPlayer ->
            connection.sendTCP(Network.Client.PlayerAdded(existingPlayer.id))
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

