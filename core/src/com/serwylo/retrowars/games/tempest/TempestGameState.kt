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
        private const val ENEMY_SPEED_INITIAL = LEVEL_DEPTH / 6f
        private const val ENEMY_SPEED_MAX = LEVEL_DEPTH / 4f

        /**
         * Bullets fired by enemies travel this much faster than [enemySpeed].
         */
        const val ENEMY_BULLET_VS_SPEED_RATIO = 2f

        /**
         * At which level will the players reach [ENEMY_SPEED_MAX].
         */
        private const val ENEMY_SPEED_MAX_LEVEL = 20

        /**
         * The time it takes to move from one segment to the next when flipping.
          */
        private const val ENEMY_FLIP_TRANSITION_TIME_INITIAL = 0.70f
        private const val ENEMY_FLIP_TRANSITION_TIME_MIN = 0.4f

        /**
         * Normally flip wait times are random (see [TIME_BETWEEN_ENEMIES_INITIAL], etc).
         * The exception is when the enemies have made it to the end of the level and are chasing
         * the player around the rim. At this time, they have consistent flip times defined below.
         */
        private const val ENEMY_FLIP_WAIT_TIME_INITIAL = 0.4f
        private const val ENEMY_FLIP_WAIT_TIME_MIN = 0.2f
        const val ENEMY_FLIP_WAIT_TIME_VARIATION = 0.2f

        private const val TIME_BETWEEN_ENEMIES_INITIAL = 1f
        private const val TIME_BETWEEN_ENEMIES_MIN = 0.5f
        const val TIME_BETWEEN_ENEMIES_VARIATION = 1f

        const val SCORE_PER_FLIPPER: Int = 3000
        const val SCORE_PER_FLIPPER_TANKER: Int = 1000

        /**
         * Wait for this many seconds after dying before spawning the next enemy at the start of
         * the level again.
         */
        const val PAUSE_AFTER_DEATH = 2f

        const val EXPLOSION_TIME = 0.35f

        const val TIME_BETWEEN_LEVELS = 2f

    }

    fun increaseSpeed() {
        val progressionTowardMax = (levelCount.toFloat() / ENEMY_SPEED_MAX_LEVEL).coerceAtMost(1f)

        enemySpeed = ENEMY_SPEED_INITIAL + (ENEMY_SPEED_MAX - ENEMY_SPEED_INITIAL) * progressionTowardMax
        enemyFlipWaitTime = ENEMY_FLIP_WAIT_TIME_INITIAL - (ENEMY_FLIP_WAIT_TIME_INITIAL - ENEMY_FLIP_WAIT_TIME_MIN) * progressionTowardMax
        enemyFlipTransitionTime = ENEMY_FLIP_TRANSITION_TIME_INITIAL - (ENEMY_FLIP_TRANSITION_TIME_INITIAL - ENEMY_FLIP_TRANSITION_TIME_MIN) * progressionTowardMax
        timeBetweenEnemies = TIME_BETWEEN_ENEMIES_INITIAL - (TIME_BETWEEN_ENEMIES_INITIAL - TIME_BETWEEN_ENEMIES_MIN) * progressionTowardMax
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

    var enemySpeed = ENEMY_SPEED_INITIAL
    var enemyFlipWaitTime = ENEMY_FLIP_WAIT_TIME_INITIAL
    var enemyFlipTransitionTime = ENEMY_FLIP_TRANSITION_TIME_INITIAL
    var timeBetweenEnemies = TIME_BETWEEN_ENEMIES_INITIAL

    var timer: Float = 0f
    var nextLevelTime: Float = 0f
    var nextPlayerRespawnTime: Float = 0f
    var nextEnemyTime: Float = 0f

    var levelCount = 0

    val poolOfFlippers = LinkedList<Flipper>()
    val poolOfFlipperTankers = LinkedList<FlipperTanker>()
    var numSpawnedFromPoolFlippers = 0
    var numSpawnedFromPoolFlipperTankers = 0

    var moveCounterClockwise = ButtonState.Unpressed
    var moveClockwise = ButtonState.Unpressed
    var fire = ButtonState.Unpressed

    var playerSegment = level.segments[0]
}

data class Explosion(
    var position: Vector3,
    var startTime: Float,
)

sealed class Enemy(
    var segment: Segment,
    var zPosition: Float,
)

/**
 * A bit strange that this extends [Enemy], but it is much easier to implement this
 * way - bullets are equally just things which move toward the player, can be hit by player
 * bullets, and have a particular behaviour when reaching the end of the screen.
 */
class EnemyBullet(
    segment: Segment,
    zPosition: Float,
): Enemy(segment, zPosition)

class Flipper(
    segment: Segment,
    zPosition: Float,

    /**
     * Number of seconds before the enemy flips to an adjacent segment in [direction].
     */
    var timeUntilNextFlip: Float,

    var direction: Direction = listOf(Direction.Clockwise, Direction.CounterClockwise).random(),
): Enemy(segment, zPosition)

class FlipperTanker(
    segment: Segment,
    zPosition: Float,
): Enemy(segment, zPosition)

enum class Direction {
    Clockwise,
    CounterClockwise,
}

fun oppositeDirection(direction: Direction) = when (direction) {
    Direction.Clockwise -> Direction.CounterClockwise
    Direction.CounterClockwise -> Direction.Clockwise
}

data class Bullet(
    val segment: Segment,
    var zPosition: Float,
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

    return Level(connectSegments(segments))

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

    return Level(connectSegments(segments))
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

    return Level(connectSegments(segments))
}

private fun connectSegments(segments: List<Segment>): List<Segment> {
    segments.forEachIndexed { i, segment ->
        segment.connect(
            segments[(i + 1) % segments.size],
            segments[(i + segments.size - 1) % segments.size]
        )
    }

    return segments
}

data class Segment(
    val start: Vector2,
    val end: Vector2,
    private var siblingClockwise: Segment? = null,
    private var siblingCounterClockwise: Segment? = null,
) {
    val centre: Vector2 = start.cpy().mulAdd(end.cpy().sub(start), 0.5f)
    val angle: Float = start.cpy().sub(end).angleDeg()

    fun offsetBy(amount: Vector2) {
        start.add(amount)
        end.add(amount)
        centre.set(start.cpy().mulAdd(end.cpy().sub(start), 0.5f))
    }

    fun next(direction: Direction) = when(direction) {
        Direction.Clockwise -> siblingClockwise!!
        Direction.CounterClockwise -> siblingCounterClockwise!!
    }

    fun connect(siblingClockwise: Segment, siblingCounterClockwise: Segment) {
        this.siblingClockwise = siblingClockwise
        this.siblingCounterClockwise = siblingCounterClockwise
    }
}

enum class ButtonState {
    Unpressed,
    JustPressed,
    Held,
}
