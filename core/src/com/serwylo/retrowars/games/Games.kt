package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen

object Games {

    val asteroids = GameDetails("game.asteroids.name") { game -> AsteroidsGameScreen(game) }

    val all = listOf(asteroids)

}

class GameDetails(
    val nameId: String,
    val createScreen: (game: RetrowarsGame) -> Screen
)