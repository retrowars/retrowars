package com.serwylo.retrowars.net

import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Listener.ThreadedListener
import com.serwylo.retrowars.net.Network.register

class RetrowarsClient {

    var me:Player? = null
    val players = mutableListOf<Player>()

    var client = Client()

    var playersChangedListener: ((List<Player>) -> Unit)? = null
    var startGameListener: (() -> Unit)? = null

    init {

        client.start()

        register(client)

        // ThreadedListener runs the listener methods on a different thread.

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(ThreadedListener(object : Listener() {
            override fun connected(connection: Connection) {}
            override fun disconnected(connection: Connection) {}
            override fun received(connection: Connection, obj: Any) {
                println("Received: $obj")
                when(obj) {
                    is Network.Client.PlayerAdded -> addPlayer(obj.id, obj.game)
                    is Network.Client.PlayerRemoved -> removePlayer(obj.id)
                    is Network.Client.PlayerScored -> onScore(obj.id, obj.score)
                    is Network.Client.StartGame -> startGameListener?.invoke()
                }
            }
        }))

        client.connect(5000, "localhost", Network.defaultPort)

    }

    private fun addPlayer(id: Long, game: String) {
        val player = Player(id, game)

        // By convention, the first player sent to a newly registered client is always themselves.
        if (players.size == 0) {
            me = player
        }

        players.add(player)
        playersChangedListener?.invoke(players.toList())
    }

    private fun removePlayer(id: Long) {
        players.removeAll { it.id == id }
        playersChangedListener?.invoke(players.toList())
    }

    private fun onScore(playerId: Long, score: Int) {
        val player = players.find { it.id == playerId } ?: return

        println("Received score for player $playerId: $score")
    }

    fun connect() = client.sendTCP(Network.Server.RegisterPlayer())

    fun updateScore(score: Int) = client.sendTCP(Network.Server.UpdateScore(score))

    fun close() {
        client.close()
    }

}

