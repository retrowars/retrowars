package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.core.UnimplementedGameScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen

object Games {

    val asteroids = GameDetails("game.asteroids.name", true) { game -> AsteroidsGameScreen(game) }
    val missileCommand = GameDetails("game.missile-command.name", false) { game -> UnimplementedGameScreen(game) }

    val all = listOf(asteroids, missileCommand)

}

class GameDetails(
    val nameId: String,
    val isAvailable: Boolean,
    val createScreen: (game: RetrowarsGame) -> Screen
)