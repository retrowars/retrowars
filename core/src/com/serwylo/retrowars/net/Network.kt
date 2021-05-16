package com.serwylo.retrowars.net

import com.esotericsoftware.kryonet.EndPoint
import com.esotericsoftware.kryonet.UdpConnection

// TODO: Send the app version code through to clients, and if there is a mismatch, prompt people to
//       upgrade to the same (latest) version.
object Network {

    const val defaultPort = 6263
    const val defaultUdpPort = 6264

    fun register(endPoint: EndPoint) {
        val kryo = endPoint.kryo;
        kryo.register(Server.RegisterPlayer::class.java)
        kryo.register(Server.UnregisterPlayer::class.java)
        kryo.register(Server.UpdateScore::class.java)
        kryo.register(Server.UpdateStatus::class.java)

        kryo.register(Client.OnPlayerAdded::class.java)
        kryo.register(Client.OnPlayerRemoved::class.java)
        kryo.register(Client.OnPlayerScored::class.java)
        kryo.register(Client.OnStartGame::class.java)
        kryo.register(Client.OnPlayerStatusChange::class.java)
        kryo.register(Client.OnPlayerReturnedToLobby::class.java)
    }

    /**
     * Messages sent *to* the [RetrowarsServer] sent *from* the [RetrowarsClient].
     */
    object Server {
        class RegisterPlayer {
            override fun toString(): String = "RegisterPlayer"
        }

        class UnregisterPlayer
        class UpdateScore(val score: Long) { constructor() : this(0) }
        class UpdateStatus(val status: String) { constructor() : this("") }
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
        class OnPlayerAdded(var id: Long, var game: String) {
            constructor() : this(0, "")
            override fun toString(): String = "OnPlayerAdded[player id: $id, game type: $game]"
        }

        class OnPlayerRemoved(var id: Long) {
            constructor() : this(0)
            override fun toString(): String = "OnPlayerRemoved[player id: $id]"
        }

        class OnPlayerScored(var id: Long, var score: Long) {
            constructor() : this(0, 0)

            override fun toString(): String = "OnPlayerScored[player id: $id, score: $score]"
        }

        class OnPlayerStatusChange(var id: Long, var status: String) {
            constructor() : this(0, "")

            override fun toString(): String = "OnPlayerStatusChange[player id: $id, status: $status]"
        }

        class OnPlayerReturnedToLobby(var id: Long, var game: String) {
            constructor() : this(0, "")

            override fun toString(): String = "OnPlayerReturnedToLobby[player id: $id, game: $game]"
        }

        class OnStartGame

    }

}