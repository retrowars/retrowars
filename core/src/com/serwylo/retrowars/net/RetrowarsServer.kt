package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.utils.AppProperties
import com.serwylo.retrowars.utils.Platform
import io.ktor.application.*
import io.ktor.client.features.json.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.random.Random


/**
 * Note that this logs both via [Gdx.app] and [Logger]. The [Logger] is so that we can get some aggregate
 * stats of how many people are using the server in a more permanent manner than the transient [Gdx.app] log.
 */
class RetrowarsServer(private val platform: Platform, private val rooms: Rooms, val port: Int) {

    sealed interface Rooms {

        fun getOrCreateRoom(requestedRoomId: Long): Room
        fun getRoomCount(): Int
        fun getPlayerCount(): Int
        fun getName(): String
        fun remove(room: Room)

        class SingleLocalRoom: Rooms {

            private val room = Room(Random.nextLong())

            override fun getOrCreateRoom(requestedRoomId: Long): Room {
                return room
            }

            override fun getRoomCount() = 1
            override fun getPlayerCount() = room.players.size
            override fun getName() = "singleLocalRoom"
            override fun remove(room: Room) {
                // Do nothing. Single room can't be removed.
            }

        }

        abstract class MultipleRooms: Rooms {

            protected val rooms = mutableListOf<Room>()

            override fun getRoomCount() = rooms.size
            override fun getPlayerCount() = rooms.sumBy { it.players.size }

            override fun remove(room: Room) {
                logger.info("Removing empty room: ${room.id}")
                rooms.remove(room)
            }

        }

        /**
         * Invite only rooms.
         * You can request a new room is created by passing in a zero for the room ID, and one will be created.
         * However, other players must obtain this room ID using some external means (e.g. QR code, SMS, etc) in order to join.
         */
        class MultiplePrivateRooms: MultipleRooms() {

            override fun getName() = "multiplePrivateRooms"

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

            override fun getName() = "publicRandomRooms"

            override fun getOrCreateRoom(requestedRoomId: Long): Room {
                val existing = rooms.find { it.players.size < maxPlayersPerRoom }

                if (existing != null) {
                    Gdx.app.log(TAG, "Adding player to existing room ${existing.id} with ${existing.players.size} other player(s).")
                    return existing
                }

                val new = Room(Random.nextLong())
                rooms.add(new)

                Gdx.app.log(TAG, "No rooms with space, so creating a new room ${new.id}.")
                logger.info("Created new room: ${new.id}")
                return new
            }

        }
    }

    companion object {

        private const val TAG = "RetrowarsServer"

        private val logger = LoggerFactory.getLogger(RetrowarsServer::class.java)

        private var server: RetrowarsServer? = null

        fun start(platform: Platform): RetrowarsServer {
            if (server != null) {
                throw IllegalStateException("Cannot start a server, one has already been started.")
            }

            val newServer = RetrowarsServer(platform, Rooms.SingleLocalRoom(), 8080)
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

        fun isEmpty(): Boolean {
            return players.isEmpty()
        }

        /**
         * If there is a game that is in progress, then the player is added in the 'pending' state.
         * This means that they will not be shown to other players who are mid-game, or even in their
         * end-game screen - only when other players return to the lobby will they see this new player.
         * However, the player who is 'pending' will be sent straight to the lobby where they can
         * watch other players scores change as the game is played.
         *
         * @return The status that is assigned to this player (see [Player.Status]) - note this will
         *         also be applied to the player which is passed here.
         */
        fun statusForNewPlayer(): String {
            if (players.any { it.status == Player.Status.playing }) {
                return Player.Status.pending
            }

            return Player.Status.lobby
        }

        fun addPlayer(player: Player) {
            players.add(player)
        }

    }

    private var server: NetworkServer
    private var connections = mutableSetOf<NetworkServer.Connection>()
    private var lastGame: Date? = null

    init {

        when (rooms) {
            is Rooms.SingleLocalRoom -> Gdx.app.log(TAG, "Starting singleton server, with only one room.")
            is Rooms.MultiplePrivateRooms -> Gdx.app.log(TAG, "Starting a server that only allows to be joined if you know the room ID.")
            is Rooms.PublicRandomRooms -> Gdx.app.log(TAG, "Starting a server that puts players into random rooms.")
        }

        logger.info("Starting server of type: ${rooms.getName()}")

        server = WebSocketNetworkServer(
            this,
            onPlayerDisconnected = {
                connections.remove(it)
                removePlayer(it)
            },
            onPlayerConnected = { connections.add(it) },
            onMessage = { obj, connection ->
                Gdx.app.log(
                    TAG,
                    "Received message [from client ${connection.player?.id}, room: ${connection.room?.id}]: $obj"
                )

                when (obj) {
                    is Network.Server.RegisterPlayer -> newPlayer(connection, obj.roomId)
                    is Network.Server.StartGame -> startGame(connection.room)
                    is Network.Server.UnregisterPlayer -> removePlayer(connection)
                    is Network.Server.UpdateScore -> updateScore(connection, obj.score)
                    is Network.Server.UpdateStatus -> updateStatus(connection, obj.status)
                }
            },
        )

        Gdx.app.log(TAG, "Starting ${rooms.getName()} server on TCP port $port")
        server.connect(platform, port, rooms.getName())
    }

    fun getRoomCount() = rooms.getRoomCount()
    fun getPlayerCount() = rooms.getPlayerCount()
    fun getLastGameTime() = lastGame
    fun getRoomType() = rooms.getName()

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

        // Don't send the message back to the player, because they already know about this status change (they requested it).
        room.sendToAllExcept(Network.Client.OnPlayerStatusChange(player.id, status), connections, allExcept)

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

        GlobalScope.launch {
            Gdx.app.debug(TAG, "Waiting for 5 seconds before telling each player to return to the main lobby for this room.")
            delay(5000)

            val newGames = room.players.associate { it.id to Games.allSupported.random().id }

            Gdx.app.debug(TAG, "Broadcasting to all players to return to the lobby and assign new games: ${newGames.map { "${it.key}: ${it.value}" }.joinToString(", ")}.")
            room.sendToAll(Network.Client.OnReturnToLobby(newGames), connections)
        }
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

        logger.info("Player removed: ${player.id}")

        if (room.isEmpty()) {
            rooms.remove(room)
        } else {
            room.sendToAll(Network.Client.OnPlayerRemoved(player.id), connections)
            checkForWinner(room)
        }

        logStats()

    }

    private fun logStats() {
        logger.info("Stats: numPlayers=${rooms.getPlayerCount()} numRooms=${rooms.getRoomCount()}")
    }

    private fun newPlayer(connection: NetworkServer.Connection, roomId: Long = 0) {
        // Ignore if already logged in.
        if (connection.player != null) {
            return
        }

        val room = rooms.getOrCreateRoom(roomId)

        // TODO: Ensure this ID doesn't already exist on the server.
        val player = Player(Random.nextLong(), Games.allSupported.random().id, room.statusForNewPlayer())

        connection.player = player

        logger.info("Player added: ${player.id}")

        connection.room = room

        // First tell people about the new player (before sending a list of all existing players to
        // this newly registered client). That means that the first PlayerAdded message received by
        // a new client will always be for themselves.
        room.sendToAll(Network.Client.OnPlayerAdded(room.id, player.id, player.game, player.status, AppProperties.appVersionCode), connections)

        // Then notify the current player about all others.
        room.players.forEach { existingPlayer ->
            connection.sendMessage(Network.Client.OnPlayerAdded(room.id, existingPlayer.id, existingPlayer.game, existingPlayer.status, AppProperties.appVersionCode))

            // If the player is waiting for a game to be completed, also let them know about the
            // scores for each player as it currently stands so they can observe.
            if (player.status == Player.Status.pending) {
                connection.sendMessage(Network.Client.OnPlayerScored(existingPlayer.id, room.scores[existingPlayer] ?: 0))
            }
        }

        room.addPlayer(player)

        logStats()
    }

    fun close() {
        val message = Network.Client.OnServerStopped()
        connections.onEach { it.sendMessage(message) }
        server.disconnect(platform)
    }

    fun startGame(room: Room?) {
        if (room == null) {
            return
        }

        Gdx.app.debug(TAG, "Server sending request to all players telling them to start the game.")
        logger.info("Starting game. Room: ${room.id}, players: [${room.players.map { it.id }.joinToString(", ")}]")
        room.scores.clear()
        room.lastGame = Date()
        room.players.onEach { it.status = Player.Status.playing }
        room.sendToAll(Network.Client.OnStartGame(), connections)

        lastGame = room.lastGame
    }

}

interface NetworkServer {
    fun connect(platform: Platform, port: Int, type: String)
    fun disconnect(platform: Platform)

    interface Connection {
        var player: Player?
        var room: RetrowarsServer.Room?

        fun sendMessage(obj: Any)
    }
}

/**
 * Note that the "discover servers on the local network" function which uses JmDNS in this class
 * could be hoisted out, given it is nothing to do with websockets. The reason it is here is because
 * the first implementation of networking using the KryoNet library had built in support for
 * discovery, so it was very much part of the "KryoNetNetworkServer". That is now gone though in
 * favour of websockets, which means we are free to make some different design decisions around
 * the discovery of servers (e.g. separating it from the protocol used to manage games).
 */
class WebSocketNetworkServer(
    private val retrowarsServer: RetrowarsServer,
    private val onMessage: (obj: Any, connection: NetworkServer.Connection) -> Unit,
    private val onPlayerDisconnected: (connection: NetworkServer.Connection) -> Unit,
    private val onPlayerConnected: (connection: NetworkServer.Connection) -> Unit,
): NetworkServer {

    companion object {

        private const val TAG = "WebSocketNetworkServer"

        /**
         * If there are any breaking changes in the server which require corresponding changes
         * in the client, then bump this version.
         *
         * When a client below this version sees this server in their list of public servers, it
         * will receive a warning. Depending on the client, it may still try and connect and hope
         * for the best, or it may suppress this server from the list (perhaps with some kind of
         * prompt about upgrading the game).
         *
         * At present, version 0.7.0 (9) is the first version with true multiplayer support at all).
         */
        private const val MIN_SUPPORTED_CLIENT_VERSION_CODE = 9
        private const val MIN_SUPPORTED_CLIENT_VERSION_NAME = "0.7.0"

    }

    internal class PlayerConnection(val session: DefaultWebSocketSession): NetworkServer.Connection {
        override var player: Player? = null
        override var room: RetrowarsServer.Room? = null

        override fun sendMessage(obj: Any) {
            runBlocking {
                val message = WebSocketMessage.toJson(obj)
                session.send(message)
            }
        }
    }

    var httpServer: NettyApplicationEngine? = null
    var jmdns: JmDNS? = null

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun connect(platform: Platform, port: Int, type: String) {
        Gdx.app.log(TAG, "Creating websocket server on port $port.")
        httpServer = embeddedServer(Netty, port) {

            install(WebSockets)
            install(ContentNegotiation) {
                gson {
                    setVersion(AppProperties.appVersionCode.toDouble())
                }
            }

            routing {

                if (type != "singleLocalRoom") {
                    get("/info") {
                        call.respond(ServerInfoDTO(
                            versionCode = AppProperties.appVersionCode,
                            versionName = AppProperties.appVersionName,
                            minSupportedClientVersionCode = MIN_SUPPORTED_CLIENT_VERSION_CODE,
                            minSupportedClientVersionName = MIN_SUPPORTED_CLIENT_VERSION_NAME,
                            type = type,
                            maxPlayersPerRoom = 5,
                            maxRooms = 10, // TODO: This isn't actually implemented yet.
                            currentRoomCount = retrowarsServer.getRoomCount(),
                            currentPlayerCount = retrowarsServer.getPlayerCount(),
                            lastGameTimestamp = retrowarsServer.getLastGameTime()?.time ?: -1,
                        ))
                    }
                }

                webSocket("/ws") {

                    Gdx.app.log(TAG, "Established new websocket connection")
                    val connection = PlayerConnection(this)
                    onPlayerConnected(connection)

                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val json = frame.readText()
                        val obj = WebSocketMessage.fromJson(json)
                        if (obj == null) {
                            Gdx.app.debug(TAG, "Ignoring unsupported message: $json")
                        } else {
                            onMessage(obj, connection)
                        }
                    }

                    Gdx.app.log(TAG, "Finished processing websocket messages. Did the client close the connection?")
                    onPlayerDisconnected(connection)
                }
            }
        }

        if (type == "singleLocalRoom") {
            scope.launch {
                runCatching {
                    Gdx.app.log(TAG, "Starting JmDNS discovery service to broadcast that we are available on port $port.")
                    platform.getMulticastControl().acquireLock()
                    jmdns = JmDNS.create(platform.getInetAddress()).apply {
                        registerService(ServiceInfo.create(
                             Network.jmdnsServiceName,
                             "localserver",
                            port,
                            "retrowars service",
                        ))
                    }
                }
            }
        }

        runBlocking {
            // Fire off in the background while we start up the JmDNS service.
            Gdx.app.debug(TAG, "Starting websocket server on port $port in a non-blocking fashion.")
            httpServer?.start()
            Gdx.app.debug(TAG, "Finished starting websocket server.")
        }

    }

    override fun disconnect(platform: Platform) {
        httpServer?.stop(1000, 1000)

        if (jmdns != null) {
            jmdns?.unregisterAllServices()
            platform.getMulticastControl().releaseLock()
            jmdns = null
        }

        runBlocking {
            job.cancelAndJoin()
        }
    }

}
