package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.core.UnimplementedGameScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.games.missilecommand.MissileCommandGameScreen
import com.serwylo.retrowars.games.snake.SnakeGameScreen
import com.serwylo.retrowars.games.spaceinvaders.SpaceInvadersGameScreen
import com.serwylo.retrowars.games.tempest.TempestGameScreen
import com.serwylo.retrowars.games.tetris.TetrisGameScreen
import com.serwylo.retrowars.input.*

object Games {

    val asteroids = GameDetails(
        "asteroids",
        isAvailable = true,
        AsteroidsSoftController(),
        { s -> s.icons.asteroids },
        { app -> AsteroidsGameScreen(app) }
    )

    val missileCommand = GameDetails(
        "missile-command",
        isAvailable = true,
        controllerLayout = null,
        { s -> s.icons.missileCommand },
        { app -> MissileCommandGameScreen(app) }
    )

    val snake = GameDetails(
        "snake",
        isAvailable = true,
        SnakeSoftController(),
        { s -> s.icons.snake },
        { app -> SnakeGameScreen(app) }
    )

    val tempest = GameDetails(
        "tempest",
        isAvailable = true,
        TempestSoftController(),
        { s -> s.icons.tempest },
        { app -> TempestGameScreen(app) }
    )

    val tetris = GameDetails(
        "tetris",
        isAvailable = true,
        TetrisSoftController(),
        { s -> s.icons.tetris },
        { app -> TetrisGameScreen(app) }
    )

    val spaceInvaders = GameDetails(
        "space-invaders",
        isAvailable = true,
        SpaceInvadersSoftController(),
        { s -> s.icons.spaceInvaders },
        { app -> SpaceInvadersGameScreen(app) }
    )

    val other = UnavailableGameDetails("other")

    val all = listOf(
        asteroids,
        missileCommand,
        snake,
        spaceInvaders,
        tempest,
        tetris,
        other,
    )

    val betaInfo = listOf<BetaInfo>()

    val allAvailable = all
        .filter { it !is UnavailableGameDetails }

    val allReleased = allAvailable
        .filter { game -> betaInfo.none { it.game === game } }

    val allBeta = allAvailable
        .filter { game -> betaInfo.any { it.game === game } }

}

class UnavailableGameDetails(name: String): GameDetails(
    name,
    isAvailable = false,
    controllerLayout = null,
    { s -> s.icons.unknown },
    { game -> UnimplementedGameScreen(game) }
)

open class GameDetails(
    val id: String,
    val isAvailable: Boolean,
    val controllerLayout: SoftControllerLayout?,
    val icon: (sprites: UiAssets.Sprites) -> TextureRegion,
    val createScreen: (app: RetrowarsGame) -> Screen
)

class BetaInfo(

    val game: GameDetails,

    val feedbackUrl: String,

    /**
     * Any known issues, plans for the future, things the player may wish to know prior to playing
     * and prior to providing feedback.
     */
    val description: String?,

)