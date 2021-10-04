package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Since
import com.serwylo.retrowars.utils.AppProperties

object Network {

    const val defaultPort = 8080
    const val jmdnsServiceName = "_retrowars._tcp.local."

    object ErrorCodes {

        /**
         * The server did not have anything interesting to tell us, wo all we can do is show the
         * user an "Unknown error occurred" or something similar. Typically this event will not
         * be sent explicitly from the server, but rather triggered by the client when something
         * happens that has not been explained by the server (e.g. client network disconnect).
         */
        const val UNKNOWN_ERROR = -1

        /**
         * The server has been shut down. In the case of a local network server, this will be
         * in response to the user who started the server finishing up and closing the server.
         * In a public server, this is not yet defined, and server shutdowns will likely not
         * be graceful enough to send this message yet (but perhaps in the future they can listen
         * for SIGTERM signals and respond with this to the client before shutting down).
         */
        const val SERVER_SHUTDOWN = 1

        /**
         * Servers will continue to accept users until a certain limit. When they have run
         * out of space, they will return this. The user can then try to reconnect in the future
         * and hope that some users have dropped out making space for them.
         */
        const val NO_ROOMS_AVAILABLE = 2

        /**
         * If the player closes their Android app (e.g. answers a call, locks the screen, anything
         * else to move it into the background), then forceably disconnect from the network and show
         * an error.
         *
         * This would not be required if the games were actually run on the server like a true
         * multiplayer game, because the game could continue in their absence. However without that,
         * it is important that we can't have some people pause the game (and hence stop receiving
         * network events that impact the game state, thus preventing themselves from being attacked
         * meaningfully).
         *
         * In the future, if we ever get to moving game state to the server, then we can do away
         * with this hack.
         *
         * NOTE: This isn't actually sent from the server, we trigger it from [GameScreen.pause] and
         *       also from [MultiplayerLobbyScreen.pause] if there is an active network connection.
          */
        const val CLIENT_CLOSED_APP = 3

        /**
         * Exceptionally unlikely, but this means that the player ID provided by a client (essentially
         * the avatar they want to use) is already in use in the current room. I say it is unlikely,
         * because the game itself only supports randomly choosing [Long] values, so it is probably
         * only going to happen if someone edits their shared preferences to a specific value that
         * clashes with someone else.
         */
        const val PLAYER_ID_IN_USE = 4

    }

    /**
     * Messages sent *to* the [RetrowarsServer] sent *from* the [RetrowarsClient].
     */
    object Server {

        /**
         * Heroku times out websockets after 55 seconds of inactivity. To avoid this, we will ping
         * periodically with an empty message.
         */
        class NetworkKeepAlive {
            override fun toString() = "NetworkKeepAlive"
        }

        /**
         * If a [roomId] is not specified, then it will request a new room to be created. If it is
         * a LAN server then it will use the default room.
         */
        class RegisterPlayer(
            @Since(9.0)
            @SerializedName("v")
            var appVersionCode: Int = 0,

            @Since(9.0)
            @SerializedName("r")
            var roomId: Long = 0,

            /**
             * Servers may choose to ignore this, and players need not provide this (e.g. if they
             * wish to remain anonymous to the server by having a new player ID each time). However,
             * by specifying a playerId, those players are able to ensure they have the same
             * Avatar each time they rejoin any server, thus making themselves more recognisable
             * to their friends and themselves.
             */
            @Since(14.0)
            @SerializedName("p")
            var playerId: Long = 0
        ) {
            override fun toString() = "RegisterPlayer [app version: $appVersionCode, room id: $roomId, player id: $playerId]"
        }

        class StartGame {
            override fun toString() = "StartGame"
        }

        class UnregisterPlayer {
            override fun toString() = "UnregisterPlayer"
        }

        class UpdateScore(
            @Since(9.0)
            @SerializedName("s")
            val score: Long
        ) {
            constructor() : this(0)

            override fun toString() = "UpdateScore [score: $score]"
        }

        class UpdateStatus(
            @Since(9.0)
            @SerializedName("s")
            val status: String
        ) {
            constructor() : this("")

            override fun toString() = "UpdateStatus [status: $status]"
        }

    }

    /**
     * Messages sent *to* the [RetrowarsClient] sent *from* the [RetrowarsServer].
     */
    object Client {

        /**
         * When a new player is registered, every other player will receive a corresponding [OnPlayerAdded]
         * message referencing the new player.
         *
         * Also, the newly added player will receive a sequence of [OnPlayerAdded] messages, one for each
         * already registered player.
         */
        class OnPlayerAdded(
            @Since(9.0)
            @SerializedName("r")
            var roomId: Long,

            @Since(9.0)
            @SerializedName("i")
            var id: Long,

            @Since(9.0)
            @SerializedName("g")
            var game: String,

            @Since(9.0)
            @SerializedName("s")
            var status: String,

            @Since(9.0)
            @SerializedName("v")
            var serverVersionCode: Int
        ) {
            constructor() : this(0, 0, "", "", 0)
            override fun toString() = "OnPlayerAdded[room id: $roomId, player id: $id, game type: $game, status: $status, server version: $serverVersionCode]"
        }

        class OnPlayerRemoved(
            @Since(9.0)
            @SerializedName("i")
            var id: Long
        ) {
            constructor() : this(0)
            override fun toString() = "OnPlayerRemoved[player id: $id]"
        }

        class OnPlayerScored(
            @Since(9.0)
            @SerializedName("i")
            var id: Long,

            @Since(9.0)
            @SerializedName("s")
            var score: Long
        ) {
            constructor() : this(0, 0)

            override fun toString() = "OnPlayerScored[player id: $id, score: $score]"
        }

        class OnPlayerStatusChange(
            @Since(9.0)
            @SerializedName("i")
            var id: Long,

            @Since(9.0)
            @SerializedName("s")
            var status: String
        ) {
            constructor() : this(0, "")

            override fun toString() = "OnPlayerStatusChange[player id: $id, status: $status]"
        }

        class OnReturnToLobby(

            /**
             * A map of [Player.id] to game names. When we return to the lobby, players are
             * assigned new games to play.
             */
            @Since(9.0)
            @SerializedName("g")
            var newGames: Map<Long, String>,

        )

        /**
         * Miscellaneous fatal errors which will cause the client to completely disconnect from the
         * server and display an error message to the user. After hitting "back", the player will
         * then be taken back to the main screen to start over all again (if they wish).
         *
         * This is generally reserved for errors which the server can explain why it occurred. That
         * is to say, if the server doesn't have enough room to accept another player, then they
         * should politely send back a [OnFatalError] message explaining this to the user.
         *
         * However if the network cuts out unexpectedly, then the server can't and shouldn't have to
         * be responsible for explaining that to the client. Therefore that will not result in a
         * [OnFatalError] and the client will have to come up with their own way of handling and
         * explaining it to the user.
         */
        class OnFatalError(

            @Since(9.0)
            @SerializedName("r")
            var code: Int,

            /**
             * Prefer the [code] over this, because that will be able to be internationalised
             * by clients. However, if a client is unable to interpret the code (e.g. it is from an
             * older client that doesn't understand that code) then it can at least fall back to
             * this message that is passed along with the error.
             */
            @Since(9.0)
            @SerializedName("m")
            var message: String,

        )

        class OnStartGame

    }

}

object WebSocketMessage {

    private const val MESSAGE_TYPE_KEY = "_m"
    private const val MESSAGE_PAYLOAD_KEY = "_p"
    private const val TAG = "WebSocketMessage"

    fun fromJson(json: String): Any? {
        val parsed = JsonParser.parseString(json).asJsonObject
        val type = parsed.get(MESSAGE_TYPE_KEY).asString
        val payload = parsed.get(MESSAGE_PAYLOAD_KEY).asJsonObject

        val gson = GsonBuilder().setVersion(AppProperties.appVersionCode.toDouble()).create()
        return when(type) {

            Network.Server.NetworkKeepAlive::class.simpleName -> Network.Server.NetworkKeepAlive()
            Network.Server.RegisterPlayer::class.simpleName -> gson.fromJson(payload, Network.Server.RegisterPlayer::class.java)
            Network.Server.UnregisterPlayer::class.simpleName -> gson.fromJson(payload, Network.Server.UnregisterPlayer::class.java)
            Network.Server.UpdateScore::class.simpleName -> gson.fromJson(payload, Network.Server.UpdateScore::class.java)
            Network.Server.UpdateStatus::class.simpleName -> gson.fromJson(payload, Network.Server.UpdateStatus::class.java)
            Network.Server.StartGame::class.simpleName -> gson.fromJson(payload, Network.Server.StartGame::class.java)

            Network.Client.OnPlayerAdded::class.simpleName -> gson.fromJson(payload, Network.Client.OnPlayerAdded::class.java)
            Network.Client.OnPlayerRemoved::class.simpleName -> gson.fromJson(payload, Network.Client.OnPlayerRemoved::class.java)
            Network.Client.OnPlayerScored::class.simpleName -> gson.fromJson(payload, Network.Client.OnPlayerScored::class.java)
            Network.Client.OnStartGame::class.simpleName -> gson.fromJson(payload, Network.Client.OnStartGame::class.java)
            Network.Client.OnPlayerStatusChange::class.simpleName -> gson.fromJson(payload, Network.Client.OnPlayerStatusChange::class.java)
            Network.Client.OnReturnToLobby::class.simpleName -> gson.fromJson(payload, Network.Client.OnReturnToLobby::class.java)
            Network.Client.OnFatalError::class.simpleName -> gson.fromJson(payload, Network.Client.OnFatalError::class.java)

            else -> {
                Gdx.app.error(TAG, "Unsupported message type: ${type}. Is this a newer client/server than we understand?")
                null
            }

        }
    }

    fun toJson(obj: Any): String {
        val gson = Gson()
        val jsonObj = JsonObject()
        jsonObj.add(MESSAGE_TYPE_KEY, JsonPrimitive(obj::class.simpleName))
        jsonObj.add(MESSAGE_PAYLOAD_KEY, gson.toJsonTree(obj))
        return gson.toJson(jsonObj)
    }

}

data class ServerHostAndPort(val host: String, val port: Int) {
    override fun toString() = "$host:$port"
}