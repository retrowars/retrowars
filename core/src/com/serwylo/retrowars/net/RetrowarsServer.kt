package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.utils.AppProperties
import java.util.*
import kotlin.random.Random

class RetrowarsServer(private val rooms: Rooms, port: Int = Network.defaultPort, udpPort: Int = Network.defaultUdpPort) {

    interface Rooms {

        fun getOrCreateRoom(requestedRoomId: Long): Room
        fun getRoomCount(): Int
        fun getPlayerCount(): Int
        fun getLastGameTime(): Date?

        class SingleRoom: Rooms {

            private val room = Room(Random.nextLong())

            override fun getOrCreateRoom(requestedRoomId: Long): Room {
                return room
            }

            override fun getRoomCount() = 1
            override fun getPlayerCount() = room.players.size
            override fun getLastGameTime() = room.lastGame
        }

        abstract class MultipleRooms: Rooms {

            protected val rooms = mutableListOf<Room>()

            override fun getRoomCount() = rooms.size
            override fun getPlayerCount() = rooms.sumBy { it.players.size }
            override fun getLastGameTime() = rooms.map { it.lastGame }.maxByOrNull { it?.time ?: 0 }

        }

        /**
         * Invite only rooms.
         * You can request a new room is created by passing in a zero for the room ID, and one will be created.
         * However, other players must obtain this room ID using some external means (e.g. QR code, SMS, etc) in order to join.
         */
        class MultiplePrivateRooms: MultipleRooms() {

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

                val newRoom = Room(Random.nextLong())
                rooms.add(newRoom)
                return newRoom
            }

        }

        /**
         * Public rooms.
         * All incoming roomId's will be ignored, and instead:
         *  - If there is space in an existing room, add the player to that room, otherwise.
         *  - Create a new room and put the player in there waiting for others to join.
         */
        class PublicRandomRooms(private val maxPlayersPerRoom: Int = 4): MultipleRooms() {

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

        val players = mutableSetOf<Player>()
        val scores = mutableMapOf<Player, Long>()

        /**
         * Remembering this date will help with both cleaning up unused rooms periodically, but also
         * reporting to users how often this server is used.
         */
        var lastGame: Date? = null

        fun sendToAll(obj: Any, allConnections: Collection<NetworkServer.Connection>) {
            sendToAllExcept(obj, allConnections, null)
        }

        fun sendToAllExcept(obj: Any, allConnections: Collection<NetworkServer.Connection>, except: NetworkServer.Connection?) {
            allConnections
                .filter { it.room?.id == id }
                .filter { except == null || except !== it }
                .onEach {

                    Gdx.app.log(TAG, "Sending message [to client ${it.player?.id}, room: ${it.room?.id}]: $obj")
                    it.sendMessage(obj)

                }
        }

    }

    private var server: NetworkServer
    private var connections = mutableSetOf<NetworkServer.Connection>()

    init {

        when (rooms) {
            is Rooms.SingleRoom -> Gdx.app.log(TAG, "Starting singleton server, with only one room.")
            is Rooms.MultiplePrivateRooms -> Gdx.app.log(TAG, "Starting a server that only allows to be joined if you know the room ID.")
            is Rooms.PublicRandomRooms -> Gdx.app.log(TAG, "Starting a server that puts players into random rooms.")
        }

        server = KryonetNetworkServer(
            port,
            udpPort,
            onPlayerDisconnected = {
                connections.remove(it)
                removePlayer(it)
           },
            onPlayerConnected = { connections.add(it) },
            onMessage = { obj, connection ->
                Gdx.app.log(TAG, "Received message [from client ${connection.player?.id}, room: ${connection.room?.id}]: $obj")

                when (obj) {
                    is Network.Server.RegisterPlayer -> newPlayer(connection, obj.roomId)
                    is Network.Server.StartGame -> startGame(connection.room)
                    is Network.Server.UnregisterPlayer -> removePlayer(connection)
                    is Network.Server.UpdateScore -> updateScore(connection, obj.score)
                    is Network.Server.UpdateStatus -> updateStatus(connection, obj.status)
                }
            },
        )

        Gdx.app.log(TAG, "Starting server on port $port (TCP) and $udpPort (UDP)")
        server.connect(port, udpPort)
    }

    fun getRoomCount() = rooms.getRoomCount()
    fun getPlayerCount() = rooms.getPlayerCount()
    fun getLastGameTime() = rooms.getLastGameTime()


    private fun updateStatus(initiatingConnection: NetworkServer.Connection, status: String) {
        val player = initiatingConnection.player
        val room = initiatingConnection.room

        updateStatus(player, room, status, initiatingConnection)
    }

    private fun updateStatus(player: Player?, room: Room?, status: String, allExcept: NetworkServer.Connection? = null) {
        if (player == null || room == null) {
            Gdx.app.error(TAG, "Ignoring updateStatus request because Player ($player) or Room ($room) is null.")
            return
        }

        player.status = status

        // If returning to the lobby, then decide on a new random game to give this player.
        if (status == Player.Status.lobby) {
            player.game = Games.allSupported.random().id
            room.sendToAll(Network.Client.OnPlayerReturnedToLobby(player.id, player.game), connections)
        } else {
            // Don't send the message back to the player, because they already know about this status change (they requested it).
            room.sendToAllExcept(Network.Client.OnPlayerStatusChange(player.id, status), connections, allExcept)
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

        val connection = connections.find { it.player?.id == survivingPlayer.id }
        if (connection == null) {
            Gdx.app.error(TAG, "Could not find connection for player ${survivingPlayer.id}, so could not ask them to return to the lobby")
            return
        }

        updateStatus(survivingPlayer, room, Player.Status.dead)
    }

    private fun updateScore(initiatingConnection: NetworkServer.Connection, score: Long) {
        val player = initiatingConnection.player
        val room = initiatingConnection.room

        if (player == null || room == null) {
            Gdx.app.error(TAG, "Ignoring updateScore request because Player ($player) or Room ($room) is null.")
            return
        }

        room.scores[player] = score

        room.sendToAllExcept(Network.Client.OnPlayerScored(player.id, score), connections, initiatingConnection)

        checkForWinner(room)
    }

    private fun removePlayer(initiatingConnection: NetworkServer.Connection) {
        val player = initiatingConnection.player
        val room = initiatingConnection.room

        if (player == null || room == null) {
            Gdx.app.error(TAG, "Ignoring removePlayer request because Player ($player) or Room ($room) is null.")
            return
        }

        room.players.remove(player)
        room.sendToAll(Network.Client.OnPlayerRemoved(player.id), connections)

        checkForWinner(room)
    }

    private fun newPlayer(connection: NetworkServer.Connection, roomId: Long = 0) {
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
        room.sendToAll(Network.Client.OnPlayerAdded(room.id, player.id, player.game, AppProperties.appVersionCode), connections)

        // Then notify the current player about all others.
        room.players.forEach { existingPlayer ->
            connection.sendMessage(Network.Client.OnPlayerAdded(room.id, existingPlayer.id, existingPlayer.game, AppProperties.appVersionCode))
        }

        room.players.add(player)
    }

    fun close() {
        val message = Network.Client.OnServerStopped()
        connections.onEach { it.sendMessage(message) }
        server.disconnect()
    }

    fun startGame(room: Room?) {
        if (room == null) {
            return
        }

        Gdx.app.debug(TAG, "Server sending request to all players telling them to start the game.")
        room.scores.clear()
        room.lastGame = Date()
        room.players.onEach { it.status = Player.Status.playing }
        room.sendToAll(Network.Client.OnStartGame(), connections)
    }

}

interface NetworkServer {
    fun connect(port: Int, udpPort: Int? = null)
    fun disconnect()

    interface Connection {
        var player: Player?
        var room: RetrowarsServer.Room?

        fun sendMessage(obj: Any)
    }
}

class KryonetNetworkServer(
    port: Int,
    udpPort: Int,
    onMessage: (obj: Any, connection: NetworkServer.Connection) -> Unit,
    onPlayerDisconnected: (connection: NetworkServer.Connection) -> Unit,
    onPlayerConnected: (connection: NetworkServer.Connection) -> Unit,
): NetworkServer {

    companion object {
        private const val TAG = "KryonetNetworkServer"
    }

    internal class PlayerConnection: Connection(), NetworkServer.Connection {
        override var player: Player? = null
        override var room: RetrowarsServer.Room? = null

        override fun sendMessage(obj: Any) {
            sendTCP(obj)
        }
    }

    private var server = object : Server() {
        override fun newConnection() = PlayerConnection()
    }

    init {

        Network.register(server)

        server.addListener(object : Listener {

            override fun connected(connection: Connection?) {
                if (connection !is PlayerConnection) {
                    return
                }

                onPlayerConnected(connection)
            }

            override fun received(connection: Connection, obj: Any) {
                if (obj is FrameworkMessage.KeepAlive || connection !is PlayerConnection) {
                    return
                }

                onMessage(obj, connection)
            }

            override fun disconnected(connection: Connection) {
                if (connection !is PlayerConnection) {
                    return
                }

                onPlayerDisconnected(connection)
            }
        })

        Gdx.app.log(TAG, "Starting server on port $port (TCP) and $udpPort (UDP)")
        server.bind(port, udpPort)
        server.start()
    }

    override fun disconnect() {
        Gdx.app.log(TAG, "Disconnecting server")
        server.stop()
        server.close()
    }

    override fun connect(port: Int, udpPort: Int?) {
        if (udpPort == null) {
            server.bind(port)
        } else {
            server.bind(port, udpPort)
        }
        server.start()
    }

}
