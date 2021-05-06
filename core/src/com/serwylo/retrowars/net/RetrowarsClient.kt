package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Listener.ThreadedListener
import com.serwylo.retrowars.net.Network.register

class RetrowarsClient {

    companion object {

        private const val TAG = "RetorwarsClient"
        private var client: RetrowarsClient? = null

        fun connect(): RetrowarsClient {
            Gdx.app.log(TAG, "Establishing connecting from client to server.")
            if (client != null) {
                throw IllegalStateException("Cannot connect to server, client connection has already been opened.")
            }

            val newClient = RetrowarsClient()
            client = newClient
            return newClient
        }

        fun get(): RetrowarsClient? = client

        fun disconnect() {
            client?.close()
            client = null
        }

    }

    val players = mutableListOf<Player>()
    val scores = mutableMapOf<Player, Long>()

    /**
     * By convention, the server always tells a client about themselves first before then passing
     * through details of all other players. Thus, the first player corresponds to the client in question.
     */
    fun me():Player? =
        if (players.size == 0) null else players[0]

    /**
     * Opposite of [me]. All players but the first.
     */
    fun otherPlayers(): List<Player> =
        if (players.size == 0) emptyList() else players.subList(1, players.size)

    var client = Client()

    var playersChangedListener: ((List<Player>) -> Unit)? = null
    var startGameListener: (() -> Unit)? = null
    var scoreChangedListener: ((player: Player, score: Long) -> Unit)? = null
    var playerDiedListener: ((player: Player) -> Unit)? = null

    init {

        client.start()

        register(client)

        client.addListener(ThreadedListener(object : Listener {
            override fun connected(connection: Connection) {}
            override fun disconnected(connection: Connection) {}
            override fun received(connection: Connection, obj: Any) {
                if (obj !is FrameworkMessage.KeepAlive) {
                    Gdx.app.log(TAG, "Received message from server: $obj")
                }

                when(obj) {
                    is Network.Client.PlayerAdded -> addPlayer(obj.id, obj.game)
                    is Network.Client.PlayerRemoved -> removePlayer(obj.id)
                    is Network.Client.PlayerScored -> onScore(obj.id, obj.score)
                    is Network.Client.PlayerDied -> onDied(obj.id)
                    is Network.Client.StartGame -> onStartGame()
                }
            }
        }))

        client.connect(5000, "localhost", Network.defaultPort)
        client.sendTCP(Network.Server.RegisterPlayer())

    }


    private fun onStartGame() {
        players.forEach { it.status = Player.Status.playing }
        startGameListener?.invoke()
    }

    private fun addPlayer(id: Long, game: String) {
        players.add(Player(id, game))
        playersChangedListener?.invoke(players.toList())
    }

    private fun removePlayer(id: Long) {
        players.removeAll { it.id == id }
        playersChangedListener?.invoke(players.toList())
    }

    private fun onScore(playerId: Long, score: Long) {
        val player = players.find { it.id == playerId } ?: return

        Gdx.app.log(TAG, "Updating player $playerId score to $score")
        scores[player] = score
        scoreChangedListener?.invoke(player, score)
    }

    private fun onDied(playerId: Long) {
        val player = players.find { it.id == playerId } ?: return

        Gdx.app.log(TAG, "Received death notice for player $playerId")
        player.status = Player.Status.dead
        playerDiedListener?.invoke(player)
    }

    fun died() {
        me()?.status = Player.Status.dead
        client.sendTCP(Network.Server.Died())
    }

    fun updateScore(score: Long) = client.sendTCP(Network.Server.UpdateScore(score))

    fun close() {
        client.close()
    }

    fun getScoreFor(player: Player): Long {
        return scores[player] ?: 0
    }

}

