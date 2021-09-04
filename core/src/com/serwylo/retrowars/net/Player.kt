package com.serwylo.retrowars.net

import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.GameDetails

class Player(

    val id: Long,

    /**
     * Lookup the corresponding [GameDetails] in [Games.all]
     */
    var game: String,

    /**
     * Covers the full lifecycle:
     *  - Waiting in a lobby pending selection of a level
     *  - Waiting in a lobby after selecting a level, waiting to start
     *  - Playing the game and still alive
     *  - Dead after the game
     *  - Lost network connection
     *  - Quit the game gracefully
     */
    var status: String,

) {

    object Status {
        /**
         * Other players are playing a game, and we joined the room midway through. Therefore, we
         * need to wait for them to complete before we are shown to them properly (i.e. don't
         * show us in the end game screen or in the in-game screen, just in the main lobby).
         */
        const val pending = "pending"
        const val lobby = "lobby"
        const val playing = "playing"
        const val dead = "dead"

        private val all = listOf(pending, lobby, playing, dead)

        fun isValid(status: String) = all.contains(status)
    }

    fun getGameDetails(): GameDetails = Games.all.find { it.id == game }!!

    override fun toString(): String = "Player[id: $id, game: $game, status: $status]"

}