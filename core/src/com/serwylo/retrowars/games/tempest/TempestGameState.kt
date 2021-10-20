package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.serwylo.retrowars.games.tetris.ButtonState
import java.util.*

class TempestGameState(private val worldWidth: Float, private val worldHeight: Float) {

    companion object {
        const val LEVEL_DEPTH = 150f
        const val BULLET_SPEED = LEVEL_DEPTH / 0.5f // Take 0.5 seconds to traverse the whole screen.

        /*
         * Take 6 seconds to traverse the whole screen, based on a crude measure
         * of https://www.youtube.com/watch?v=jfaCrdBABUY from 0:22 until 0:28 in the top right.
         */
        const val ENEMY_SPEED = LEVEL_DEPTH / 6f

        /**
         * The time it takes to move from one segment to the next when crawling.
          */
        const val ENEMY_CRAWL_TRANSITION_TIME = 0.5f


        const val ENEMY_CRAWL_WAIT_TIME = 0.75f

        const val MIN_TIME_BETWEEN_ENEMIES = 0.5f
        const val MAX_TIME_BETWEEN_ENEMIES = 2f

        const val SCORE_PER_ENEMY: Int = 4000

        const val BASE_ENEMIES_PER_LEVEL = 10

        /**
         * Wait for this many seconds after dying before spawning the next enemy at the start of
         * the level again.
         */
        const val PAUSE_AFTER_DEATH = 2f

        const val EXPLOSION_TIME = 0.35f

        const val TIME_BETWEEN_LEVELS = 2f

    }

    val bullets = LinkedList<Bullet>()
    val enemies = LinkedList<Enemy>()
    val explosions = LinkedList<Explosion>()

    var numLives = 3

    val allLevels = listOf(
        makeFirstLevel(worldWidth, worldHeight),
        makeSecondLevel(worldWidth, worldHeight),
        makeThirdLevel(worldWidth, worldHeight),
    )

    var level = allLevels[0]

    var timer: Float = 0f
    var nextLevelTime: Float = 0f
    var nextPlayerRespawnTime: Float = 0f
    var nextEnemyTime: Float = 0f
    var numEnemiesRemaining: Int = BASE_ENEMIES_PER_LEVEL

    var levelCount = 0

    var moveCounterClockwise = ButtonState.Unpressed
    var moveClockwise = ButtonState.Unpressed
    var fire = ButtonState.Unpressed

    var playerSegment = level.segments[0]

    fun shouldSpawnEnemy() = timer > nextEnemyTime
}

data class Explosion(
    var position: Vector3,
    var startTime: Float,
)

fun makeEnemy(segment: Segment) = Enemy(
    segment,
    depth = TempestGameState.LEVEL_DEPTH,
    timeUntilNextCrawl = TempestGameState.ENEMY_CRAWL_WAIT_TIME,
)

data class Enemy(
    var segment: Segment,
    var depth: Float,

    /**
     * Number of seconds before the enemy crawls around the end of the level.
     */
    var timeUntilNextCrawl: Float = TempestGameState.ENEMY_CRAWL_WAIT_TIME,

    var crawlFraction: Float = 0f,
    var direction: Direction = listOf(Direction.Clockwise, Direction.CounterClockwise).random(),

) {

    enum class Direction {
        Clockwise,
        CounterClockwise,
    }
}

data class Bullet(
    val segment: Segment,
    var depth: Float,
)

data class Level(
    val segments: List<Segment>,
)

/**
 * A round level with 16 segments. Start at the center of the screen and project outwards in a
 * straight line, then rotate 16 times around a 360 degree circle, recording one segment each time.
 *
 * You can see the original level this is modelled off here: https://youtu.be/jfaCrdBABUY?t=19
 */
private fun makeFirstLevel(worldWidth: Float, worldHeight: Float): Level {

    val approxDegreesBetween = 360f / 16
    val startingAngle = (Math.random() * 360).toFloat()
    val length = worldWidth.coerceAtMost(worldHeight) / 2 * 0.8f
    val center = Vector2(worldWidth / 2, worldHeight / 2)
    val segments = (0 until 16).map { i ->

        val start = Vector2(0f, length).rotateDeg(startingAngle + i * approxDegreesBetween)
        val end = Vector2(0f, length).rotateDeg(startingAngle + (i + 1) * approxDegreesBetween)

        Segment(start, end).apply { offsetBy(center) }

    }

    return Level(segments)

}

/**
 * A square, with 4 segments on each size.
 *
 * https://youtu.be/jfaCrdBABUY?t=34
 */
private fun makeSecondLevel(worldWidth: Float, worldHeight: Float): Level {
    val length = worldWidth.coerceAtMost(worldHeight) * 0.8f / 4
    val center = Vector2(worldWidth / 2, worldHeight / 2)

    val segments = listOf(
        Segment(Vector2(-2 * length, -2 * length), Vector2(-length, -2 * length)),
        Segment(Vector2(-length, -2 * length),        Vector2(0f, -2 * length)),
        Segment(Vector2(0f, -2 * length),          Vector2(length, -2 * length)),
        Segment(Vector2(length, -2 * length),         Vector2(2 * length, -2 * length)),

        Segment(Vector2(2 * length, -2 * length), Vector2(2 * length, -length)),
        Segment(Vector2(2 * length, -length),        Vector2(2 * length, 0f)),
        Segment(Vector2(2 * length, 0f),          Vector2(2 * length, length)),
        Segment(Vector2(2 * length, length),         Vector2(2 * length, 2 * length)),

        Segment(Vector2(length, 2 * length),         Vector2(2 * length, 2 * length)),
        Segment(Vector2(0f, 2 * length),          Vector2(length, 2 * length)),
        Segment(Vector2(-length, 2 * length),        Vector2(0f, 2 * length)),
        Segment(Vector2(-2 * length, 2 * length), Vector2(-length, 2 * length)),

        Segment(Vector2(-2 * length, length),         Vector2(-2 * length, 2 * length)),
        Segment(Vector2(-2 * length, 0f),          Vector2(-2 * length, length)),
        Segment(Vector2(-2 * length, -length),        Vector2(-2 * length, 0f)),
        Segment(Vector2(-2 * length, -2 * length), Vector2(-2 * length, -length)),
    )

    segments.forEach {
        it.offsetBy(center)
    }

    return Level(segments)
}

/**
 * A cross, where each line in the cross is two segments wide, and extends one segment past the center.
 *
 * https://youtu.be/jfaCrdBABUY?t=34
 */
private fun makeThirdLevel(worldWidth: Float, worldHeight: Float): Level {
    val length = worldWidth.coerceAtMost(worldHeight) * 0.8f / 4
    val center = Vector2(worldWidth / 2, worldHeight / 2)

    val segments = listOf(
        Segment(Vector2(-length, -length), Vector2(-length, -2 * length)),
        Segment(Vector2(-length, -2 * length),        Vector2(0f, -2 * length)),
        Segment(Vector2(0f, -2 * length),          Vector2(length, -2 * length)),
        Segment(Vector2(length, -2 * length),         Vector2(length, -length)),

        Segment(Vector2(length, -length), Vector2(2 * length, -length)),
        Segment(Vector2(2 * length, -length),        Vector2(2 * length, 0f)),
        Segment(Vector2(2 * length, 0f),          Vector2(2 * length, length)),
        Segment(Vector2(2 * length, length),         Vector2(length, length)),

        Segment(Vector2(length, length),         Vector2(length, 2 * length)),
        Segment(Vector2(length, 2 * length),          Vector2(0f, 2 * length)),
        Segment(Vector2(0f, 2 * length),        Vector2(-length, 2 * length)),
        Segment(Vector2(-length, 2 * length), Vector2(-length, length)),

        Segment(Vector2(-length, length),         Vector2(-2 * length, length)),
        Segment(Vector2(-2 * length, length),        Vector2(-2 * length, 0f)),
        Segment(Vector2(-2 * length, 0f),          Vector2(-2 * length, -length)),
        Segment(Vector2(-2 * length, -length), Vector2(-length, -length)),
    )

    segments.forEach {
        it.offsetBy(center)
    }

    return Level(segments)
}

data class Segment(
    val start: Vector2,
    val end: Vector2,
) {
    val centre: Vector2 = start.cpy().mulAdd(end.cpy().sub(start), 0.5f)
    val angle: Float = start.cpy().sub(end).angleDeg()

    fun offsetBy(amount: Vector2) {
        start.add(amount)
        end.add(amount)
        centre.set(start.cpy().mulAdd(end.cpy().sub(start), 0.5f))
    }
}

enum class ButtonState {
    Unpressed,
    JustPressed,
    Held,
}
