package com.serwylo.retrowars.net

import com.esotericsoftware.kryonet.EndPoint

object Network {

    const val defaultPort = 6263

    fun register(endPoint: EndPoint) {
        val kryo = endPoint.kryo;
        kryo.register(Server.RegisterPlayer::class.java)
        kryo.register(Server.UnregisterPlayer::class.java)
        kryo.register(Server.UpdateScore::class.java)
        kryo.register(Client.PlayerAdded::class.java)
        kryo.register(Client.PlayerRemoved::class.java)
        kryo.register(Client.PlayerScored::class.java)
        kryo.register(Client.StartGame::class.java)
    }

    object Server {
        class RegisterPlayer
        class UnregisterPlayer
        class UpdateScore(var score: Int) { constructor() : this(0) }
    }

    object Client {

        /**
         * When a new player is registered, every other player will receive a corresponding [PlayerAdded]
         * message referencing the new player.
         *
         * Also, the newly added player will receive a sequence of [PlayerAdded] messages, one for each
         * already registered player.
         */
        class PlayerAdded(var id: Long) { constructor() : this(0) }
        class PlayerRemoved(var id: Long) { constructor() : this(0) }
        class PlayerScored(var id: Long, var score: Int) { constructor() : this(0, 0) }
        class StartGame

    }

}