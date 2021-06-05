package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.core.UnimplementedGameScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.games.missilecommand.MissileCommandGameScreen
import com.serwylo.retrowars.games.snake.SnakeGameScreen
import com.serwylo.retrowars.games.tetris.TetrisGameScreen

object Games {

    val asteroids = GameDetails(
        "asteroids",
        true,
        { s -> s.icons.asteroids },
        { app, _ -> AsteroidsGameScreen(app) }
    )

    val missileCommand = GameDetails(
        "missile-command",
        true,
        { s -> s.icons.missileCommand },
        { app, _ -> MissileCommandGameScreen(app) }
    )

    val snake = GameDetails(
        "snake",
        true,
        { s -> s.icons.snake },
        { app, _ -> SnakeGameScreen(app) }
    )

    val tetris = GameDetails(
        "tetris",
        true,
        { s -> s.icons.tetris },
        { app, _ -> TetrisGameScreen(app) }
    )

    val other = UnavailableGameDetails("other")

    val all = listOf(
        asteroids,
        missileCommand,
        snake,
        tetris,
        other,
    )

    val allSupported = all.filter { it !is UnavailableGameDetails }

}

class UnavailableGameDetails(name: String): GameDetails(
    name,
    false,
    { s -> s.icons.unknown },
    { game, details -> UnimplementedGameScreen(game, details) }
)

open class GameDetails(
    val id: String,
    val isAvailable: Boolean,
    val icon: (sprites: UiAssets.Sprites) -> TextureRegion,
    val createScreen: (app: RetrowarsGame, gameDetails: GameDetails) -> Screen
)