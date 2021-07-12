package com.serwylo.retrowars.net

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName

// TODO: Send the app version code through to clients, and if there is a mismatch, prompt people to
//       upgrade to the same (latest) version.
object Network {

    const val defaultPort = 6263
    const val defaultUdpPort = defaultPort + 1

    /**
     * Messages sent *to* the [RetrowarsServer] sent *from* the [RetrowarsClient].
     */
    object Server {

        /**
         * If a [roomId] is not specified, then it will request a new room to be created. If it is
         * a LAN server then it will use the default room.
         */
        class RegisterPlayer(
            @SerializedName("v")
            var appVersionCode: Int = 0,

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
            @SerializedName("s")
            val score: Long
        ) {
            constructor() : this(0)

            override fun toString() = "UpdateScore [score: $score]"
        }

        class UpdateStatus(
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
            @SerializedName("r")
            var roomId: Long,

            @SerializedName("i")
            var id: Long,

            @SerializedName("g")
            var game: String,

            @SerializedName("v")
            var serverVersionCode: Int
        ) {
            constructor() : this(0, 0, "", 0)
            override fun toString() = "OnPlayerAdded[room id: $roomId, player id: $id, game type: $game, server version: $serverVersionCode]"
        }

        class OnPlayerRemoved(
            @SerializedName("i")
            var id: Long
        ) {
            constructor() : this(0)
            override fun toString() = "OnPlayerRemoved[player id: $id]"
        }

        class OnPlayerScored(
            @SerializedName("i")
            var id: Long,

            @SerializedName("s")
            var score: Long
        ) {
            constructor() : this(0, 0)

            override fun toString() = "OnPlayerScored[player id: $id, score: $score]"
        }

        class OnPlayerStatusChange(
            @SerializedName("i")
            var id: Long,

            @SerializedName("s")
            var status: String
        ) {
            constructor() : this(0, "")

            override fun toString() = "OnPlayerStatusChange[player id: $id, status: $status]"
        }

        class OnPlayerReturnedToLobby(
            @SerializedName("i")
            var id: Long,

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

    fun fromJson(json: String): Any {
        val parsed = JsonParser.parseString(json).asJsonObject
        val type = parsed.get(MESSAGE_TYPE_KEY).asString
        val payload = parsed.get(MESSAGE_PAYLOAD_KEY).asJsonObject
        return when(type) {

            Network.Server.RegisterPlayer::class.simpleName -> Gson().fromJson(payload, Network.Server.RegisterPlayer::class.java)
            Network.Server.UnregisterPlayer::class.simpleName -> Gson().fromJson(payload, Network.Server.UnregisterPlayer::class.java)
            Network.Server.UpdateScore::class.simpleName -> Gson().fromJson(payload, Network.Server.UpdateScore::class.java)
            Network.Server.UpdateStatus::class.simpleName -> Gson().fromJson(payload, Network.Server.UpdateStatus::class.java)
            Network.Server.StartGame::class.simpleName -> Gson().fromJson(payload, Network.Server.StartGame::class.java)

            Network.Client.OnPlayerAdded::class.simpleName -> Gson().fromJson(payload, Network.Client.OnPlayerAdded::class.java)
            Network.Client.OnPlayerRemoved::class.simpleName -> Gson().fromJson(payload, Network.Client.OnPlayerRemoved::class.java)
            Network.Client.OnPlayerScored::class.simpleName -> Gson().fromJson(payload, Network.Client.OnPlayerScored::class.java)
            Network.Client.OnStartGame::class.simpleName -> Gson().fromJson(payload, Network.Client.OnStartGame::class.java)
            Network.Client.OnPlayerStatusChange::class.simpleName -> Gson().fromJson(payload, Network.Client.OnPlayerStatusChange::class.java)
            Network.Client.OnPlayerReturnedToLobby::class.simpleName -> Gson().fromJson(payload, Network.Client.OnPlayerReturnedToLobby::class.java)
            Network.Client.OnServerStopped::class.simpleName -> Gson().fromJson(payload, Network.Client.OnServerStopped::class.java)

            else -> throw IllegalStateException("Unsupported message type: ${type}. Is this a newer client/server than we understand?")

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
