package com.serwylo.retrowars.net

import com.esotericsoftware.kryonet.EndPoint

// TODO: Send the app version code through to clients, and if there is a mismatch, prompt people to
//       upgrade to the same (latest) version.
object Network {

    const val defaultPort = 6263

    fun register(endPoint: EndPoint) {
        val kryo = endPoint.kryo;
        kryo.register(Server.RegisterPlayer::class.java)
        kryo.register(Server.UnregisterPlayer::class.java)
        kryo.register(Server.UpdateScore::class.java)
        kryo.register(Server.UpdateStatus::class.java)
        kryo.register(Client.PlayerAdded::class.java)
        kryo.register(Client.PlayerRemoved::class.java)
        kryo.register(Client.PlayerScored::class.java)
        kryo.register(Client.StartGame::class.java)
        kryo.register(Client.PlayerStatusChange::class.java)
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
         * When a new player is registered, every other player will receive a corresponding [PlayerAdded]
         * message referencing the new player.
         *
         * Also, the newly added player will receive a sequence of [PlayerAdded] messages, one for each
         * already registered player.
         */
        class PlayerAdded(var id: Long, var game: String) {
            constructor() : this(0, "")
            override fun toString(): String = "PlayerAdded[player id: $id, game type: $game]"
        }

        class PlayerRemoved(var id: Long) {
            constructor() : this(0)
            override fun toString(): String = "PlayerRemoved[player id: $id]"
        }

        class PlayerScored(var id: Long, var score: Long) {
            constructor() : this(0, 0)

            override fun toString(): String = "PlayerScored[player id: $id, score: $score]"
        }

        class PlayerStatusChange(var id: Long, var status: String) {
            constructor() : this(0, "")

            override fun toString(): String = "PlayerStatusChange[player id: $id, status: $status]"
        }

        class StartGame

    }

}