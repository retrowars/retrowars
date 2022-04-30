package com.serwylo.retrowars.games

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.core.UnimplementedGameScreen
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
import com.serwylo.retrowars.games.breakout.BreakoutGameScreen
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
        "music/go_go_go.mp3",
        "game.asteroids.intro-message.positive",
        "game.asteroids.intro-message.negative",
        { s -> s.icons.asteroids },
        { app -> AsteroidsGameScreen(app) }
    )

    val breakout = GameDetails(
        "breakout",
        isAvailable = true,
        null,
        "music/where_am_i.mp3",
        "game.breakout.intro-message.positive",
        "game.breakout.intro-message.negative",
        { s -> s.icons.breakout },
        { app -> BreakoutGameScreen(app) }
    )

    val missileCommand = GameDetails(
        "missile-command",
        isAvailable = true,
        controllerLayout = null,
        "music/go_go_faster.mp3",
        "game.missile-command.intro-message.positive",
        "game.missile-command.intro-message.negative",
        { s -> s.icons.missileCommand },
        { app -> MissileCommandGameScreen(app) }
    )

    val snake = GameDetails(
        "snake",
        isAvailable = true,
        SnakeSoftController(),
        "music/tutorial.mp3",
        "game.snake.intro-message.positive",
        "game.snake.intro-message.negative",
        { s -> s.icons.snake },
        { app -> SnakeGameScreen(app) }
    )

    val tempest = GameDetails(
        "tempest",
        isAvailable = true,
        TempestSoftController(),
        "music/go_go_metallius.mp3",
        "game.tempest.intro-message.positive",
        "game.tempest.intro-message.negative",
        { s -> s.icons.tempest },
        { app -> TempestGameScreen(app) }
    )

    val tetris = GameDetails(
        "tetris",
        isAvailable = true,
        TetrisSoftController(),
        "music/metallius.mp3",
        "game.tetris.intro-message.positive",
        "game.tetris.intro-message.negative",
        { s -> s.icons.tetris },
        { app -> TetrisGameScreen(app) }
    )

    val spaceInvaders = GameDetails(
        "space-invaders",
        isAvailable = true,
        SpaceInvadersSoftController(),
        "music/wherever_aliens.mp3",
        "game.space-invaders.intro-message.positive",
        "game.space-invaders.intro-message.negative",
        { s -> s.icons.spaceInvaders },
        { app -> SpaceInvadersGameScreen(app) }
    )

    val other = UnavailableGameDetails("other")

    val all = listOf(
        asteroids,
        breakout,
        missileCommand,
        snake,
        spaceInvaders,
        tempest,
        tetris,
        other,
    )

    val betaInfo = listOf(
        BetaInfo(
            breakout,
            "https://github.com/retrowars/retrowars/issues/new?assignees=&labels=Game:%20Breakout,Beta%20Feedback&template=&title=",
            """
            Although in early development, the future roadmap includes:
             - Multiplayer support
            """.trimIndent()
        )
    )

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
    "",
    "",
    "",
    { s -> s.icons.unknown },
    { game -> UnimplementedGameScreen(game) }
)

open class GameDetails(
    val id: String,
    val isAvailable: Boolean,
    val controllerLayout: SoftControllerLayout?,
    val songAsset: String,
    val positiveDescription: String,
    val negativeDescription: String,
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