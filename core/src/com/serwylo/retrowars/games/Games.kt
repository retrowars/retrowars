package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.core.UnimplementedGameScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.games.missilecommand.MissileCommandGameScreen
import com.serwylo.retrowars.games.snake.SnakeGameScreen
import com.serwylo.retrowars.games.tempest.TempestGameScreen
import com.serwylo.retrowars.games.tetris.TetrisGameScreen
import com.serwylo.retrowars.input.*

object Games {

    val asteroids = GameDetails(
        "asteroids",
        isAvailable = true,
        isBeta = false,
        AsteroidsSoftController(),
        { s -> s.icons.asteroids },
        { app -> AsteroidsGameScreen(app) }
    )

    val missileCommand = GameDetails(
        "missile-command",
        isAvailable = true,
        isBeta = false,
        controllerLayout = null,
        { s -> s.icons.missileCommand },
        { app -> MissileCommandGameScreen(app) }
    )

    val snake = GameDetails(
        "snake",
        isAvailable = true,
        isBeta = false,
        SnakeSoftController(),
        { s -> s.icons.snake },
        { app -> SnakeGameScreen(app) }
    )

    val tempest = GameDetails(
        "tempest",
        isAvailable = true,
        isBeta = true,
        TempestSoftController(),
        { s -> s.icons.tempest },
        { app -> TempestGameScreen(app) }
    )

    val tetris = GameDetails(
        "tetris",
        isAvailable = true,
        isBeta = false,
        TetrisSoftController(),
        { s -> s.icons.tetris },
        { app -> TetrisGameScreen(app) }
    )

    val other = UnavailableGameDetails("other")

    val all = listOf(
        asteroids,
        missileCommand,
        snake,
        tempest,
        tetris,
        other,
    )

    val allSupported = all.filter { it !is UnavailableGameDetails }

}

class UnavailableGameDetails(name: String): GameDetails(
    name,
    isAvailable = false,
    isBeta = false,
    controllerLayout = null,
    { s -> s.icons.unknown },
    { game -> UnimplementedGameScreen(game) }
)

open class GameDetails(
    val id: String,
    val isAvailable: Boolean,
    val isBeta: Boolean,
    val controllerLayout: SoftControllerLayout?,
    val icon: (sprites: UiAssets.Sprites) -> TextureRegion,
    val createScreen: (app: RetrowarsGame) -> Screen
)