package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.core.UnimplementedGameScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen

object Games {

    val asteroids = GameDetails("game.asteroids.name", true) { app, _ -> AsteroidsGameScreen(app) }

    val missileCommand = GameDetails("game.missile-command.name", false) { game, details -> UnimplementedGameScreen(game, details) }
    val spaceInvaders = GameDetails("game.space-invaders.name", false) { game, details -> UnimplementedGameScreen(game, details) }
    val snake = GameDetails("game.snake.name", false) { game, details -> UnimplementedGameScreen(game, details) }

    val all = listOf(asteroids, missileCommand, spaceInvaders, snake)

}

class GameDetails(
    val nameId: String,
    val isAvailable: Boolean,
    val createScreen: (app: RetrowarsGame, gameDetails: GameDetails) -> Screen
)