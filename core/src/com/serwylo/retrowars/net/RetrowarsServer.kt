package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.esotericsoftware.minlog.Log
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
    private val scores = mutableMapOf<Player, Long>()

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

        server.bind(Network.defaultPort, Network.defaultUdpPort)
        server.start()
    }

    private fun updateStatus(player: Player?, status: String) {
        if (player == null) {
            return
        }

        player.status = status

        // If returning to the lobby, then decide on a new random game to give this player.
        if (status == Player.Status.lobby) {
            player.game = Games.allSupported.random().id
            server.sendToAllTCP(Network.Client.OnPlayerReturnedToLobby(player.id, player.game))
        } else {
            server.sendToAllTCP(Network.Client.OnPlayerStatusChange(player.id, status))
        }

        checkForWinner()

    }

    /**
     * If there is only one player left (all the rest are either in the end game screen or removed
     * removed from the game), and the sole remaining player has the highest score, tell them to
     * die so that we can all return to the end game screen and celebrate.
     *
     * Letting that player go on forever smashing everyone else in scores will be not very fun for
     * the others to watch, especially because they can't actually watch the game, only the score.
     */
    private fun checkForWinner() {
        val stillPlaying = players.filter { it.status == Player.Status.playing }

        if (stillPlaying.size != 1) {
            return
        }

        val survivingPlayer = stillPlaying[0]

        val highestScore = scores.maxByOrNull { it.value }?.value ?: 0
        val playersWithHighestScore = scores.filterValues { it == highestScore }.keys

        if (playersWithHighestScore.size != 1 || !playersWithHighestScore.contains(survivingPlayer)) {
            return
        }

        Gdx.app.log(TAG, "Only one player remaining and their score is the highest. Ask them to end their game so we can all continue playing a new game.")

        val connection = server.connections.find { (it as PlayerConnection).player?.id == survivingPlayer.id }
        if (connection == null) {
            Gdx.app.error(TAG, "Could not find connection for player ${survivingPlayer.id}, so could not ask them to return to the lobby")
            return
        }

        updateStatus(survivingPlayer, Player.Status.dead)
    }

    private fun updateScore(player: Player?, score: Long) {
        if (player == null) {
            return
        }

        scores[player] = score

        // TODO: Don't send back to the client that originally reported their own score.
        server.sendToAllTCP(Network.Client.OnPlayerScored(player.id, score))

        checkForWinner()
    }

    private fun removePlayer(player: Player?) {
        if (player == null) {
            return
        }

        players.remove(player)
        server.sendToAllTCP(Network.Client.OnPlayerRemoved(player.id))

        checkForWinner()
    }


    private fun newPlayer(connection: PlayerConnection) {
        // Ignore if already logged in.
        if (connection.player != null) {
            return
        }

        // TODO: Ensure this ID doesn't already exist on the server.
        val player = Player(Random.nextLong(), Games.allSupported.random().id)

        connection.player = player

        // First tell people about the new player (before sending a list of all existing players to
        // this newly registered client). That means that the first PlayerAdded message received by
        // a new client will always be for themselves.
        server.sendToAllTCP(Network.Client.OnPlayerAdded(player.id, player.game))

        // Then notify the current player about all others.
        players.forEach { existingPlayer ->
            connection.sendTCP(Network.Client.OnPlayerAdded(existingPlayer.id, existingPlayer.game))
        }

        players.add(player)
    }

    fun close() {
        server.sendToAllTCP(Network.Client.OnServerStopped())
        server.close()
    }

    fun startGame() {
        scores.clear()
        players.onEach { it.status = Player.Status.playing }
        server.sendToAllTCP(Network.Client.OnStartGame())
    }

}

