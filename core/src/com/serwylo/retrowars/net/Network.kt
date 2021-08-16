package com.serwylo.retrowars.net

import com.badlogic.gdx.Gdx
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Since
import com.serwylo.retrowars.utils.AppProperties

object Network {

    const val defaultPort = 8080
    const val jmdnsServiceName = "_retrowars._tcp.local."

    /**
     * Messages sent *to* the [RetrowarsServer] sent *from* the [RetrowarsClient].
     */
    object Server {

        /**
         * Heroku times out websockets after 55 seconds of inactivity. To avoid this, we will ping
         * periodically with an empty message.
         */
        class Ping

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
            var roomId: Long = 0
        ) {
            override fun toString() = "RegisterPlayer [app version: $appVersionCode, room id; $roomId]"
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
            @SerializedName("v")
            var serverVersionCode: Int
        ) {
            constructor() : this(0, 0, "", 0)
            override fun toString() = "OnPlayerAdded[room id: $roomId, player id: $id, game type: $game, server version: $serverVersionCode]"
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

        class OnPlayerReturnedToLobby(
            @Since(9.0)
            @SerializedName("i")
            var id: Long,

            @Since(9.0)
            @SerializedName("g")
            var game: String
        ) {
            constructor() : this(0, "")

            override fun toString() = "OnPlayerReturnedToLobby[player id: $id, game: $game]"
        }

        class OnStartGame
        class OnServerStopped

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

            Network.Server.Ping::class.simpleName -> Network.Server.Ping()
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
            Network.Client.OnPlayerReturnedToLobby::class.simpleName -> gson.fromJson(payload, Network.Client.OnPlayerReturnedToLobby::class.java)
            Network.Client.OnServerStopped::class.simpleName -> gson.fromJson(payload, Network.Client.OnServerStopped::class.java)

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
