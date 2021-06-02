package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.net.Network.register
import com.serwylo.retrowars.utils.AppProperties
import kotlin.random.Random

class RetrowarsServer(private val rooms: Rooms, private val port: Int = Network.defaultPort, private val udpPort: Int = Network.defaultUdpPort) {

    interface Rooms {

        fun getOrCreateRoom(requestedRoomId: Long): Room

        class SingleRoom: Rooms {

            private val room = Room(Random.nextLong())

            override fun getOrCreateRoom(requestedRoomId: Long): Room {
                return room
            }

        }

        /**
         * Invite only rooms.
         * You can request a new room is created by passing in a zero for the room ID, and one will be created.
         * However, other players must obtain this room ID using some external means (e.g. QR code, SMS, etc) in order to join.
         */
        class MultiplePrivateRooms: Rooms {

            private val rooms = listOf<Room>()

            override fun getOrCreateRoom(requestedRoomId: Long): Room {
                if (requestedRoomId > 0) {
                    val existing = rooms.find { it.id == requestedRoomId }
                    if (existing != null) {
                        Gdx.app.log(TAG, "Adding player to existing room $requestedRoomId at their request.")
                        return existing
                    } else {
                        Gdx.app.error(TAG, "Asked to join room $requestedRoomId, but it could not be found. We will ignore that request, and put them in a new room instead.")
                    }
                } else {
                    Gdx.app.log(TAG, "Creating a new room at players request.")
                }

                return Room(Random.nextLong())
            }

        }

        /**
         * Public rooms.
         * All incoming roomId's will be ignored, and instead:
         *  - If there is space in an existing room, add the player to that room, otherwise.
         *  - Create a new room and put the player in there waiting for others to join.
         */
        class PublicRandomRooms(private val maxPlayersPerRoom: Int = 4): Rooms {

            private val rooms = mutableListOf<Room>()

            override fun getOrCreateRoom(requestedRoomId: Long): Room {
                val existing = rooms.find { it.players.size < maxPlayersPerRoom }

                if (existing != null) {
                    Gdx.app.log(TAG, "Adding player to existing room ${existing.id} with ${existing.players.size} other player(s).")
                    return existing
                }

                val new = Room(Random.nextLong())
                rooms.add(new)

                Gdx.app.log(TAG, "No rooms with space, so creating a new room ${new.id}.")
                return new
            }

        }
    }

    companion object {

        private const val TAG = "RetrowarsServer"

        private var server: RetrowarsServer? = null

        fun start(): RetrowarsServer {
            if (server != null) {
                throw IllegalStateException("Cannot start a server, one has already been started.")
            }

            val newServer = RetrowarsServer(Rooms.SingleRoom())
            server = newServer
            return newServer
        }

        fun get(): RetrowarsServer? = server

        fun stop() {
            server?.close()
            server = null
        }

    }

    class Room(val id: Long) {
        var players = mutableSetOf<Player>()
        val scores = mutableMapOf<Player, Long>()

        fun sendToAllTCP(obj: Any, allConnections: Collection<Connection>) {
            allConnections
                .filter { it is PlayerConnection && it.room?.id == id}
                .onEach { it.sendTCP(obj) }
        }
    }

    private var server = object : Server() {
        override fun newConnection() = PlayerConnection()
    }

    // This holds per connection state.
    internal class PlayerConnection: Connection() {
        var player: Player? = null
        var room: Room? = null
    }

    init {

        register(server)

        when (rooms) {
            is Rooms.SingleRoom -> Gdx.app.log(TAG, "Starting singleton server, with only one room.")
            is Rooms.MultiplePrivateRooms -> Gdx.app.log(TAG, "Starting a server that only allows to be joined if you know the room ID.")
            is Rooms.PublicRandomRooms -> Gdx.app.log(TAG, "Starting a server that puts players into random rooms.")
        }

        server.addListener(object : Listener {
            override fun received(c: Connection, obj: Any) {
                val connection = c as PlayerConnection
                if (obj !is FrameworkMessage.KeepAlive) {
                    Gdx.app.log(TAG, "Received message from client: $obj")
                }

                when (obj) {
                    is Network.Server.RegisterPlayer -> newPlayer(connection, obj.roomId)
                    is Network.Server.StartGame -> startGame(connection.room)
                    is Network.Server.UnregisterPlayer -> removePlayer(connection.player, connection.room)
                    is Network.Server.UpdateScore -> updateScore(connection.player, connection.room, obj.score)
                    is Network.Server.UpdateStatus -> updateStatus(connection.player, connection.room, obj.status)
                }
            }

            override fun disconnected(c: Connection) {
                if (c !is PlayerConnection) {
                    return
                }

                removePlayer(c.player, c.room)
            }
        })

        server.bind(port, udpPort)
        server.start()
    }

    private fun updateStatus(player: Player?, room: Room?, status: String) {
        if (player == null || room == null) {
            Gdx.app.error(TAG, "Ignoring updateStatus request because Player ($player) or Room ($room) is null.")
            return
        }

        player.status = status

        // If returning to the lobby, then decide on a new random game to give this player.
        if (status == Player.Status.lobby) {
            player.game = Games.tetris.id // Games.allSupported.random().id
            room.sendToAllTCP(Network.Client.OnPlayerReturnedToLobby(player.id, player.game), server.connections)
        } else {
            room.sendToAllTCP(Network.Client.OnPlayerStatusChange(player.id, status), server.connections)
        }

        checkForWinner(room)

    }

    /**
     * If there is only one player left (all the rest are either in the end game screen or removed
     * removed from the game), and the sole remaining player has the highest score, tell them to
     * die so that we can all return to the end game screen and celebrate.
     *
     * Letting that player go on forever smashing everyone else in scores will be not very fun for
     * the others to watch, especially because they can't actually watch the game, only the score.
     */
    private fun checkForWinner(room: Room) {
        val stillPlaying = room.players.filter { it.status == Player.Status.playing }

        if (stillPlaying.size != 1) {
            return
        }

        val survivingPlayer = stillPlaying[0]

        val highestScore = room.scores.maxByOrNull { it.value }?.value ?: 0
        val playersWithHighestScore = room.scores.filterValues { it == highestScore }.keys

        if (playersWithHighestScore.size != 1 || !playersWithHighestScore.contains(survivingPlayer)) {
            return
        }

        Gdx.app.log(TAG, "Only one player remaining and their score is the highest. Ask them to end their game so we can all continue playing a new game.")

        val connection = server.connections.find { (it as PlayerConnection).player?.id == survivingPlayer.id }
        if (connection == null) {
            Gdx.app.error(TAG, "Could not find connection for player ${survivingPlayer.id}, so could not ask them to return to the lobby")
            return
        }

        updateStatus(survivingPlayer, room, Player.Status.dead)
    }

    private fun updateScore(player: Player?, room: Room?, score: Long) {
        if (player == null || room == null) {
            Gdx.app.error(TAG, "Ignoring updateScore request because Player ($player) or Room ($room) is null.")
            return
        }

        room.scores[player] = score

        // TODO: Don't send back to the client that originally reported their own score.
        server.sendToAllTCP(Network.Client.OnPlayerScored(player.id, score))

        checkForWinner(room)
    }

    private fun removePlayer(player: Player?, room: Room?) {
        if (player == null || room == null) {
            Gdx.app.error(TAG, "Ignoring removePlayer request because Player ($player) or Room ($room) is null.")
            return
        }

        room.players.remove(player)
        server.sendToAllTCP(Network.Client.OnPlayerRemoved(player.id))

        checkForWinner(room)
    }

    private fun newPlayer(connection: PlayerConnection, roomId: Long = 0) {
        // Ignore if already logged in.
        if (connection.player != null) {
            return
        }

        // TODO: Ensure this ID doesn't already exist on the server.
        val player = Player(Random.nextLong(), Games.allSupported.random().id)

        connection.player = player

        val room = rooms.getOrCreateRoom(roomId)

        connection.room = room

        // First tell people about the new player (before sending a list of all existing players to
        // this newly registered client). That means that the first PlayerAdded message received by
        // a new client will always be for themselves.
        room.sendToAllTCP(Network.Client.OnPlayerAdded(room.id, player.id, player.game, AppProperties.appVersionCode), server.connections)

        // Then notify the current player about all others.
        room.players.forEach { existingPlayer ->
            connection.sendTCP(Network.Client.OnPlayerAdded(room.id, existingPlayer.id, existingPlayer.game, AppProperties.appVersionCode))
        }

        room.players.add(player)
    }

    fun close() {
        server.sendToAllTCP(Network.Client.OnServerStopped())
        server.close()
    }

    fun startGame(room: Room?) {
        if (room == null) {
            return
        }

        Gdx.app.debug(TAG, "Server sending request to all players telling them to start the game.")
        room.scores.clear()
        room.players.onEach { it.status = Player.Status.playing }
        room.sendToAllTCP(Network.Client.OnStartGame(), server.connections)
    }

}

