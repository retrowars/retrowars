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
        kryo.register(Server.Died::class.java)
        kryo.register(Client.PlayerAdded::class.java)
        kryo.register(Client.PlayerRemoved::class.java)
        kryo.register(Client.PlayerScored::class.java)
        kryo.register(Client.StartGame::class.java)
        kryo.register(Client.PlayerDied::class.java)
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
        class Died
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

        // TODO: Make this happen as part of a general "health" that each game sends through.
        //       Sure, most games don't really have a notion of health from 0-100, but we could
        //       estimate. e.g. Asteroids is 33% per life, whereas missilecommand is the number of
        //       bases left. If you want to get very extravagent, then it could be the nubmer of
        //       bases left, but augmented by how much ammunition and how many enemy missiles are
        //       left. That would result in a nice little jostle within a game, whereby a player
        //       can start off badly by wasting missiles, only for a later recovery afterwards.
        class PlayerDied(var id: Long) {
            constructor() : this(0)

            override fun toString(): String = "PlayerDied[player id]"
        }

        class StartGame

    }

}