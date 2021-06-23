package com.serwylo.retrowars.net

import com.esotericsoftware.kryonet.EndPoint
import com.google.gson.Gson

// TODO: Send the app version code through to clients, and if there is a mismatch, prompt people to
//       upgrade to the same (latest) version.
object Network {

    const val defaultPort = 6263
    const val defaultUdpPort = defaultPort + 1

    fun register(endPoint: EndPoint) {
        val kryo = endPoint.kryo
        kryo.register(Server.RegisterPlayer::class.java)
        kryo.register(Server.UnregisterPlayer::class.java)
        kryo.register(Server.UpdateScore::class.java)
        kryo.register(Server.UpdateStatus::class.java)
        kryo.register(Server.StartGame::class.java)

        kryo.register(Client.OnPlayerAdded::class.java)
        kryo.register(Client.OnPlayerRemoved::class.java)
        kryo.register(Client.OnPlayerScored::class.java)
        kryo.register(Client.OnStartGame::class.java)
        kryo.register(Client.OnPlayerStatusChange::class.java)
        kryo.register(Client.OnPlayerReturnedToLobby::class.java)
        kryo.register(Client.OnServerStopped::class.java)
    }

    /**
     * Messages sent *to* the [RetrowarsServer] sent *from* the [RetrowarsClient].
     */
    object Server {

        /**
         * If a [roomId] is not specified, then it will request a new room to be created. If it is
         * a LAN server then it will use the default room.
         */
        class RegisterPlayer(var appVersionCode: Int = 0, var roomId: Long = 0) {
            override fun toString() = "RegisterPlayer [app version: $appVersionCode, room id; $roomId]"
        }

        class StartGame {
            override fun toString() = "StartGame"
        }

        class UnregisterPlayer {
            override fun toString() = "UnregisterPlayer"
        }

        class UpdateScore(val score: Long) {
            constructor() : this(0)

            override fun toString() = "UpdateScore [score: $score]"
        }

        class UpdateStatus(val status: String) {
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
        class OnPlayerAdded(var roomId: Long, var id: Long, var game: String, var serverVersionCode: Int) {
            constructor() : this(0, 0, "", 0)
            override fun toString() = "OnPlayerAdded[room id: $roomId, player id: $id, game type: $game, server version: $serverVersionCode]"
        }

        class OnPlayerRemoved(var id: Long) {
            constructor() : this(0)
            override fun toString() = "OnPlayerRemoved[player id: $id]"
        }

        class OnPlayerScored(var id: Long, var score: Long) {
            constructor() : this(0, 0)

            override fun toString() = "OnPlayerScored[player id: $id, score: $score]"
        }

        class OnPlayerStatusChange(var id: Long, var status: String) {
            constructor() : this(0, "")

            override fun toString() = "OnPlayerStatusChange[player id: $id, status: $status]"
        }

        class OnPlayerReturnedToLobby(var id: Long, var game: String) {
            constructor() : this(0, "")

            override fun toString() = "OnPlayerReturnedToLobby[player id: $id, game: $game]"
        }

        class OnStartGame
        class OnServerStopped

    }

}