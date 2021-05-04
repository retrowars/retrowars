package com.serwylo.retrowars.net

import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.GameDetails

class Player(
    val id: Long,

    /**
     * Lookup the corresponding [GameDetails] in [Games.all]
     */
    val game: String
) {

    fun getGameDetails(): GameDetails = Games.all[game]!!

    override fun toString(): String = "Player[id: $id, game: $game]"

}