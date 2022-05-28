package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import java.lang.IllegalStateException
import java.util.*

class TempestGameState(worldWidth: Float, worldHeight: Float) {

    companion object {
        const val LEVEL_DEPTH = 200f
        const val BULLET_SPEED = LEVEL_DEPTH / 0.5f // Take 0.5 seconds to traverse the whole screen.

        const val MAX_PLAYER_BULLETS_AT_ONCE = 8

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
        const val SCORE_PER_SPIKE_BUILDER: Int = 1000

        const val SPIKE_LENGTH_LOSS_PER_HIT = LEVEL_DEPTH / 50f

        /**
         * Wait for this many seconds after dying before spawning the next enemy at the start of
         * the level again.
         */
        const val PAUSE_AFTER_DEATH = 2f

        const val EXPLOSION_TIME = 0.35f

        const val TOTAL_TIME_BETWEEN_LEVELS = 2.5f
        const val LEVEL_END_TRANSIT_TIME = 2f

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

    /**
     * If an enemy is in here (in addition to the [enemies] list), then it came from an attack
     * in a multiplayer game. This will be rendered differently to the player.
     */
    val networkEnemies = mutableSetOf<Enemy>()

    /**
     * If we received an attack while transitioning between levels, then the enemies that were
     * spawned as a result will be recorded such that they can be applied at the start of the
     * next level (or the start of the current level if we hit a spike while transitioning).
     */
    var numQueuedNetworkEnemies = 0

    var numLives = 3

    val allLevels = listOf(
        makeRoundLevel(worldWidth, worldHeight),
        makeSquareLevel(worldWidth, worldHeight),
        makeCrossLevel(worldWidth, worldHeight),
        makeGogglesLevel(worldWidth, worldHeight),
        makeRoundedCrossLevel(worldWidth, worldHeight),
        makeTriangleLevel(worldWidth, worldHeight),
        makeAngledCrossLevel(worldWidth, worldHeight),
        makeVLevel(worldWidth, worldHeight),
        makeSteppedVLevel(worldWidth, worldHeight),
        makeULevel(worldWidth, worldHeight),
        makeFlatLevel(worldWidth, worldHeight),
        makeHeartLevel(worldWidth, worldHeight),
        makeStarLevel(worldWidth, worldHeight),
        makeWLevel(worldWidth, worldHeight),
        makeUpsideDownMountainLevel(worldWidth, worldHeight),
        makeFigureEightLevel(worldWidth, worldHeight),
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
    val poolOfSpikeBuilders = LinkedList<SpikeBuilder>()
    var numSpawnedFromPoolFlippers = 0
    var numSpawnedFromPoolFlipperTankers = 0
    var numSpawnedFromPoolSpikeBuilder = 0

    var playerSegment = level.segments[0]
    var playerDepth = 0f
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

    var direction: Direction = randomDirection(segment),

    /**
     * Used only to the purpose of initiating a sound when the flipping starts. Not actually
     * used to figure out if we are flipping or not because that can trivially be done by
     * consulting [timeUntilNextFlip] and comparing it to [TempestGameState.enemyFlipWaitTime].
     */
    var isFlipping: Boolean = false,
): Enemy(segment, zPosition) {

    init {
        if (segment.next(direction) == null) {
            throw IllegalStateException("Oops")
        }
    }

    companion object {

        private fun randomDirection(currentSegment: Segment): Direction {
            val potentialNextDirection = listOf(Direction.CounterClockwise, Direction.Clockwise).random()
            return if (currentSegment.next(potentialNextDirection) != null) {
                potentialNextDirection
            } else {
                oppositeDirection(potentialNextDirection)
            }
        }

    }

}


class FlipperTanker(
    segment: Segment,
    zPosition: Float = TempestGameState.LEVEL_DEPTH,
): Enemy(segment, zPosition)

class Spike(
    segment: Segment,
    zPosition: Float,
): Enemy(segment, zPosition)

class SpikeBuilder(
    segment: Segment,
    zPosition: Float = TempestGameState.LEVEL_DEPTH,
    val spike: Spike = Spike(segment, TempestGameState.LEVEL_DEPTH),
    var direction: ZDirection = ZDirection.Advancing,
): Enemy(segment, zPosition) {
    companion object {
        val vertices = floatArrayOf(
            1.3717206f, 0.70842665f,
            1.4630519f, 2.0407734f,
            0.32039535f, 2.69244f,
            -0.98175764f, 2.2703066f,
            -0.48876882f, 1.5640967f,
            0.2748803f, 1.3464468f,
            0.9370318f, 1.6608467f,
            1.2125256f, 2.2993166f,
            0.8500339f, 3.0635567f,
            0.30236346f, 3.2039268f,
            -0.14560995f, 2.8120368f,
            -0.09727795f, 2.3089967f,
            0.1443831f, 2.1735666f,
            0.35045266f, 2.2039666f,
            0.43061566f, 2.3863566f,
            0.3023626f, 2.4924965f,
        )
    }
}

enum class ZDirection {
    Advancing,
    Retreating,
}

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
    val cameraOffset: Float,
)

private fun defaultCameraOffset(worldHeight: Float) = - worldHeight / 8f

/**
 * From this SVG path:
 *
 * https://youtu.be/TgZc7G8AtVU?t=592
 *
 */
private fun makeUpsideDownMountainLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.3f

    val vertices = listOf(
        Vector2(-90.09062f, 143.08424f),
        Vector2(-85.328125f, 129.19362f),
        Vector2(-82.68229f, 113.71549f),
        Vector2(-81.22708f, 96.64987f),
        Vector2(-78.581245f, 79.45195f),
        Vector2(-75.00937f, 65.16445f),
        Vector2(-67.99791f, 48.892582f),
        Vector2(-59.266663f, 57.491543f),
        Vector2(-48.55104f, 54.581123f),
        Vector2(-43.523956f, 69.1332f),
        Vector2(-40.87812f, 87.38945f),
        Vector2(-35.98333f, 102.4707f),
        Vector2(-23.94479f, 101.0155f),
        Vector2(-20.505207f, 118.21342f),
        Vector2(-17.065628f, 135.41132f),
        Vector2(-13.626048f, 152.60922f),
    )

    return Level(connectSegmentsInLine(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), worldHeight / 3f)
}

/**
 * From this SVG path: m 362.81167,71.705317 4.637,12.126043 0.55851,14.388578 1.38238,12.997422 7.62027,8.95307 h 11.7583 l 8.67061,-8.6021 4.45854,-12.730192 h 10.43538 l 4.45854,12.730192 8.67061,8.6021 h 11.75829 l 7.62029,-8.95307 1.38237,-12.997422 0.55851,-14.388578 4.637,-12.126043
 *
 * https://youtu.be/TgZc7G8AtVU?t=561
 *
 */
private fun makeWLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.35f

    val vertices = listOf(
        Vector2(362.81168f, 120.25547f),
        Vector2(367.44867f, 108.12943f),
        Vector2(368.00717f, 93.74085f),
        Vector2(369.38956f, 80.74343f),
        Vector2(377.00983f, 71.79036f),
        Vector2(388.76813f, 71.79036f),
        Vector2(397.43875f, 80.392456f),
        Vector2(401.89728f, 93.12265f),
        Vector2(412.33267f, 93.12265f),
        Vector2(416.7912f, 80.392456f),
        Vector2(425.46182f, 71.79036f),
        Vector2(437.22012f, 71.79036f),
        Vector2(444.84042f, 80.74343f),
        Vector2(446.22278f, 93.74085f),
        Vector2(446.78128f, 108.12943f),
        Vector2(451.41827f, 120.25547f),
    )

    return Level(connectSegmentsInLine(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), worldHeight / 1.8f)
}

/**
 * From this SVG path: m 312.53905,116.02827 6.41615,19.05 16.40417,-1.32291 5.15938,-21.82813 -0.26459,-24.473955 -4.10104,-23.283326 -11.1125,-17.33021 -15.1474,-6.35 -15.1474,6.35 -11.1125,17.33021 -4.10104,23.283326 -0.26459,24.473955 5.15938,21.82813 16.40417,1.32291 6.41615,-19.05
 *
 * https://youtu.be/TgZc7G8AtVU?t=510
 *
 */
private fun makeHeartLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.35f

    val vertices = listOf(
        Vector2(312.53906f, 116.02827f),
        Vector2(318.9552f, 135.07826f),
        Vector2(335.35938f, 133.75536f),
        Vector2(340.51877f, 111.92722f),
        Vector2(340.25418f, 87.45327f),
        Vector2(336.15314f, 64.169945f),
        Vector2(325.04065f, 46.839737f),
        Vector2(309.89325f, 40.48974f),
        Vector2(294.74585f, 46.839737f),
        Vector2(283.63336f, 64.169945f),
        Vector2(279.53232f, 87.45327f),
        Vector2(279.26773f, 111.92722f),
        Vector2(284.42712f, 133.75536f),
        Vector2(300.8313f, 135.07826f),
        Vector2(307.24744f, 116.02826f),
    )

    return Level(connectSegmentsInLine(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), defaultCameraOffset(worldHeight))
}

/**
 * From this SVG path:
 *
 * U shape: https://youtu.be/TgZc7G8AtVU?t=448
 *
 */
private fun makeULevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.35f

    val vertices = listOf(
        Vector2(173.87833f, 113.0802f),
        Vector2(173.87833f, 99.332146f),
        Vector2(173.87833f, 85.58409f),
        Vector2(173.87833f, 71.83604f),
        Vector2(174.98201f, 58.13233f),
        Vector2(178.79681f, 44.92389f),
        Vector2(186.2837f, 33.392162f),
        Vector2(198.2176f, 26.561672f),
        Vector2(211.96877f, 26.561672f),
        Vector2(223.90266f, 33.392162f),
        Vector2(231.38956f, 44.92389f),
        Vector2(235.20436f, 58.13233f),
        Vector2(236.30804f, 71.83604f),
        Vector2(236.30804f, 85.58409f),
        Vector2(236.30804f, 99.332146f),
        Vector2(236.30804f, 113.0802f),
    )

    return Level(connectSegmentsInLine(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), worldHeight / 1.8f)
}

/**
 * From this SVG path: m 12.038516,62.446872 v 18.520836 h 13.229172 v 18.520836 h 13.229167 v 18.520836 h 13.229172 v 18.52083 H 64.955199 V 118.00938 H 78.184371 V 99.488544 H 91.413538 V 80.967708 H 104.64271 V 62.446872
 *
 * V shape but each side is a set of steps (slightly taller than they are wide): htts://youtu.be/TgZc7G8AtVU?t=398
 *
 */
private fun makeSteppedVLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.5f

    val vertices = listOf(
        Vector2(12.038516f, 136.6625f),
        Vector2(12.038516f, 118.14167f),
        Vector2(25.267689f, 118.14167f),
        Vector2(25.267689f, 99.62083f),
        Vector2(38.496857f, 99.62083f),
        Vector2(38.496857f, 81.09999f),
        Vector2(51.72603f, 81.09999f),
        Vector2(51.72603f, 62.579163f),
        Vector2(64.9552f, 62.579163f),
        Vector2(64.9552f, 81.09999f),
        Vector2(78.18437f, 81.09999f),
        Vector2(78.18437f, 99.62083f),
        Vector2(91.413536f, 99.62083f),
        Vector2(91.413536f, 118.14166f),
        Vector2(104.64271f, 118.14166f),
        Vector2(104.64271f, 136.66249f),
    )

    return Level(connectSegmentsInLine(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), worldHeight / 2f)
}

/**
 * From this SVG path: m 424.7478,410.27629 -4.01963,-14.16916 -4.01963,-14.16917 -4.01962,-14.16917 -4.01963,-14.16916 -4.01962,-14.16917 -4.01963,-14.16916 -4.02196,-14.16916 -9.69285,-0.20093 -4.01963,14.16917 -4.01963,14.16916 -4.01962,14.16917 -4.01963,14.16916 -4.01962,14.16917 -4.01963,14.16917 -4.01963,14.16916
 *
 * V shape with a flat bottom: htts://youtu.be/TgZc7G8AtVU?t=398
 *
 */
private fun makeVLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.3f

    val vertices = listOf(
        Vector2(358.7779f, 410.07535f),
        Vector2(362.79752f, 395.9062f),
        Vector2(366.81714f, 381.73703f),
        Vector2(370.83676f, 367.56787f),
        Vector2(374.85638f, 353.3987f),
        Vector2(378.876f, 339.22955f),
        Vector2(382.89563f, 325.0604f),
        Vector2(386.91525f, 310.89124f),
        Vector2(396.6081f, 311.09216f),
        Vector2(400.63007f, 325.26132f),
        Vector2(404.6497f, 339.43048f),
        Vector2(408.6693f, 353.59964f),
        Vector2(412.68893f, 367.7688f),
        Vector2(416.70856f, 381.93796f),
        Vector2(420.72818f, 396.10712f),
        Vector2(424.7478f, 410.27628f),
    )

    return Level(connectSegmentsInLine(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), defaultCameraOffset(worldHeight))
}

/**
 * A round level with 16 segments. Start at the center of the screen and project outwards in a
 * straight line, then rotate 16 times around a 360 degree circle, recording one segment each time.
 *
 * You can see the original level this is modelled off here: https://youtu.be/jfaCrdBABUY?t=19
 */
private fun makeRoundLevel(worldWidth: Float, worldHeight: Float): Level {

    val approxDegreesBetween = 360f / 16
    val startingAngle = (Math.random() * 360).toFloat()
    val length = worldWidth.coerceAtMost(worldHeight) / 2 * 0.8f
    val center = Vector2(worldWidth / 2, worldHeight / 2)
    val segments = (0 until 16).map { i ->

        val start = Vector2(0f, length).rotateDeg(startingAngle + i * -approxDegreesBetween)
        val end = Vector2(0f, length).rotateDeg(startingAngle + (i + 1) * -approxDegreesBetween)

        Segment(start, end).apply { offsetBy(center) }

    }

    return Level(connectSegmentsInLoop(segments), defaultCameraOffset(worldHeight))

}

/**
 * Flat plane: https://youtu.be/TgZc7G8AtVU?t=485
 */
private fun makeFlatLevel(worldWidth: Float, worldHeight: Float): Level {
    val length = worldWidth.coerceAtMost(worldHeight) * 0.8f / 7
    val left = (worldWidth / 2) - length * 8

    val segments = (0 until 16).map { i ->
        Segment(
            Vector2(left + i * length, 0f),
            Vector2(left + (i + 1) * length, 0f),
        )
    }

    return Level(connectSegmentsInLine(segments), defaultCameraOffset(worldHeight))
}

/**
 * A square, with 4 segments on each size.
 *
 * https://youtu.be/jfaCrdBABUY?t=34
 */
private fun makeSquareLevel(worldWidth: Float, worldHeight: Float): Level {
    val length = worldWidth.coerceAtMost(worldHeight) * 0.8f / 4
    val center = Vector2(worldWidth / 2, worldHeight / 2)

    val segments = listOf(

        Segment(Vector2(-2 * length, -2 * length), Vector2(-2 * length, -length)),
        Segment(Vector2(-2 * length, -length),        Vector2(-2 * length, 0f)),
        Segment(Vector2(-2 * length, 0f),          Vector2(-2 * length, length)),
        Segment(Vector2(-2 * length, length),         Vector2(-2 * length, 2 * length)),

        Segment(Vector2(-2 * length, 2 * length), Vector2(-length, 2 * length)),
        Segment(Vector2(-length, 2 * length),        Vector2(0f, 2 * length)),
        Segment(Vector2(0f, 2 * length),          Vector2(length, 2 * length)),
        Segment(Vector2(length, 2 * length),         Vector2(2 * length, 2 * length)),

        Segment(Vector2(2 * length, length),         Vector2(2 * length, 2 * length)),
        Segment(Vector2(2 * length, 0f),          Vector2(2 * length, length)),
        Segment(Vector2(2 * length, -length),        Vector2(2 * length, 0f)),
        Segment(Vector2(2 * length, -2 * length), Vector2(2 * length, -length)),

        Segment(Vector2(length, -2 * length),         Vector2(2 * length, -2 * length)),
        Segment(Vector2(0f, -2 * length),          Vector2(length, -2 * length)),
        Segment(Vector2(-length, -2 * length),        Vector2(0f, -2 * length)),
        Segment(Vector2(-2 * length, -2 * length), Vector2(-length, -2 * length)),

    )

    segments.forEach {
        it.offsetBy(center)
    }

    return Level(connectSegmentsInLoop(segments), defaultCameraOffset(worldHeight))
}

/**
 * https://youtu.be/TgZc7G8AtVU?t=621
 *
 * From this SVG path: m 277.5992,361.6662 17.0266,10.7454 17.0227,-10.7515 4.7552,-19.4287 -4.7552,-19.4287 -17.0227,-10.7515 -17.0266,10.7454 -5.0751,19.4347 -5.0751,19.4347 -17.0266,10.7454 -17.0227,-10.7515 -4.7552,-19.4287 4.7552,-19.4287 17.0227,-10.7515 17.0266,10.7454 5.0751,19.4347 5.0751,19.4347 z
 */
private fun makeFigureEightLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.4f

    val vertices = listOf(
        Vector2(277.5992f, 361.6662f),
        Vector2(294.62582f, 372.4116f),
        Vector2(311.64853f, 361.6601f),
        Vector2(316.40372f, 342.23138f),
        Vector2(311.64853f, 322.80267f),
        Vector2(294.62582f, 312.05118f),
        Vector2(277.5992f, 322.79657f),
        Vector2(272.5241f, 342.23126f),
        Vector2(267.449f, 361.66595f),
        Vector2(250.42241f, 372.41135f),
        Vector2(233.3997f, 361.65985f),
        Vector2(228.6445f, 342.23114f),
        Vector2(233.3997f, 322.80243f),
        Vector2(250.42241f, 312.05093f),
        Vector2(267.449f, 322.79633f),
        Vector2(272.5241f, 342.23102f),
        Vector2(277.5992f, 361.6657f),
        Vector2(277.5992f, 361.6662f),
    )

    return Level(connectSegmentsInLoop(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), 0f)
}

/**
 * A cross, but tilted 45 degrees and with slightly tapered or rounded corners.
 *
 * https://youtu.be/TgZc7G8AtVU?t=372
 *
 * From this SVG path: m 410.75459,244.03871 20.90208,2.91042 6.87917,-20.90208 6.87917,20.90208 20.90208,-2.91042 2.91042,-20.90208 -15.6104,-14.81666 15.6104,-14.81666 -2.91042,-20.90208 -20.90208,-2.91042 -6.87917,20.90208 -6.87917,-20.90208 -20.90208,2.91042 -2.91042,20.90208 15.6104,14.81666 -15.6104,14.81666 z
 */
private fun makeAngledCrossLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.4f

    val vertices = listOf(
        Vector2(410.75458f, 244.03871f),
        Vector2(431.65665f, 246.94913f),
        Vector2(438.53583f, 226.04704f),
        Vector2(445.415f, 246.94913f),
        Vector2(466.31708f, 244.03871f),
        Vector2(469.2275f, 223.13663f),
        Vector2(453.6171f, 208.31996f),
        Vector2(469.2275f, 193.5033f),
        Vector2(466.31708f, 172.60121f),
        Vector2(445.415f, 169.6908f),
        Vector2(438.53583f, 190.59288f),
        Vector2(431.65665f, 169.6908f),
        Vector2(410.75458f, 172.60121f),
        Vector2(407.84415f, 193.5033f),
        Vector2(423.45456f, 208.31996f),
        Vector2(407.84415f, 223.13663f),
        Vector2(410.75458f, 244.03871f),
    )

    return Level(connectSegmentsInLoop(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), defaultCameraOffset(worldHeight))
}

/**
 * A triangle, with five segments along the two upright sides, and six along the bottom.
 *
 * https://youtu.be/TgZc7G8AtVU?t=344
 *
 * From this SVG path: m 308.8181,238.002 6.22385,-19.76036 6.22386,-19.76039 6.22386,-19.76037 6.22386,-19.76038 6.22386,19.76038 6.22385,19.76037 6.22386,19.76038 6.22385,19.76037 6.22386,19.76038 h -10.3731 -10.37309 -10.37309 -10.3731 -10.37309 -10.3731 z
 */
private fun makeTriangleLevel(worldWidth: Float, worldHeight: Float): Level {

    // Note that the first vertex is on a long edge of the triangle with slightly longer
    // edges than the bottom. When calling makeLevelFromVertices, it will scale the entire
    // shape so the first vertex matches targetLength.
    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.4f

    val vertices = listOf(
        Vector2(308.8181f, 238.002f),
        Vector2(302.59418f, 257.7624f),
        Vector2(312.9673f, 257.7624f),
        Vector2(323.34036f, 257.7624f),
        Vector2(333.71347f, 257.7624f),
        Vector2(344.08655f, 257.7624f),
        Vector2(354.45963f, 257.7624f),
        Vector2(364.83273f, 257.7624f),
        Vector2(358.6089f, 238.002f),
        Vector2(352.38504f, 218.24162f),
        Vector2(346.1612f, 198.48125f),
        Vector2(339.93735f, 178.72087f),
        Vector2(333.7135f, 158.9605f),
        Vector2(327.48965f, 178.72087f),
        Vector2(321.2658f, 198.48125f),
        Vector2(315.04196f, 218.24164f),
        Vector2(308.8181f, 238.002f),
    )

    return Level(connectSegmentsInLoop(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), defaultCameraOffset(worldHeight))
}

/**
 * A cross, where each line in the cross is two segments wide, and extends one segment past the center.
 *
 * https://youtu.be/jfaCrdBABUY?t=34
 */
private fun makeCrossLevel(worldWidth: Float, worldHeight: Float): Level {
    val length = worldWidth.coerceAtMost(worldHeight) * 0.8f / 4
    val center = Vector2(worldWidth / 2, worldHeight / 2)

    val segments = listOf(

        Segment(Vector2(-2 * length, -length), Vector2(-length, -length)),
        Segment(Vector2(-2 * length, 0f),          Vector2(-2 * length, -length)),
        Segment(Vector2(-2 * length, length),        Vector2(-2 * length, 0f)),
        Segment(Vector2(-length, length),         Vector2(-2 * length, length)),

        Segment(Vector2(-length, 2 * length), Vector2(-length, length)),
        Segment(Vector2(0f, 2 * length),        Vector2(-length, 2 * length)),
        Segment(Vector2(length, 2 * length),          Vector2(0f, 2 * length)),
        Segment(Vector2(length, length),         Vector2(length, 2 * length)),

        Segment(Vector2(2 * length, length),         Vector2(length, length)),
        Segment(Vector2(2 * length, 0f),          Vector2(2 * length, length)),
        Segment(Vector2(2 * length, -length),        Vector2(2 * length, 0f)),
        Segment(Vector2(length, -length), Vector2(2 * length, -length)),

        Segment(Vector2(length, -2 * length),         Vector2(length, -length)),
        Segment(Vector2(0f, -2 * length),          Vector2(length, -2 * length)),
        Segment(Vector2(-length, -2 * length),        Vector2(0f, -2 * length)),
        Segment(Vector2(-length, -length), Vector2(-length, -2 * length)),

    )

    segments.forEach {
        it.offsetBy(center)
    }

    return Level(connectSegmentsInLoop(segments), defaultCameraOffset(worldHeight))
}

/**
 * Given a list of vertices, normalise somewhat by:
 *  - Moving so that they are centered around the origin.
 *  - Scaling so that the length of each segment is [segmentLength].
 *
 *  Assumptions:
 *   - Each pair of vertices represents a line fo the same length.
 *   - The last vertex is the same as the first (forming a closed loop). TODO: Subject to change.
 */
private fun makeLevelFromVertices(vertices: List<Vector2>, segmentLength: Float, worldWidth: Float, worldHeight: Float): List<Segment> {

    val minX = vertices.minByOrNull { it.x }?.x ?: 0f
    val maxX = vertices.maxByOrNull { it.x }?.x ?: 0f
    val minY = vertices.minByOrNull { it.y }?.y ?: 0f
    val maxY = vertices.maxByOrNull { it.y }?.y ?: 0f

    val width = maxX - minX
    val height = maxY - minY

    val actualLength = vertices[1].dst(vertices[0])

    val lengthMultiplier = segmentLength / actualLength
    vertices.forEach {
        it.x -= minX + width / 2
        it.y -= minY + height / 2

        it.x *= lengthMultiplier
        it.y *= lengthMultiplier
    }

    val center = Vector2(worldWidth / 2, worldHeight / 2)

    return (0 until vertices.size - 1).map { i ->
        Segment(vertices[i].cpy(), vertices[i + 1].cpy()).apply { offsetBy(center) }
    }

}

/**
 * https://youtu.be/TgZc7G8AtVU?t=293
 */
private fun makeGogglesLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.5f

    val vertices = listOf(

        Vector2(108.183395f, 71.973f),
        Vector2(101.263504f, 59.71124f),
        Vector2(87.827545f, 55.502872f),
        Vector2(76.729164f, 64.16667f),
        Vector2(62.64955f, 64.16667f),
        Vector2(51.551163f, 55.502872f),
        Vector2(38.115204f, 59.71124f),
        Vector2(31.195314f, 71.973f),
        Vector2(31.195314f, 86.05259f),
        Vector2(38.115204f, 98.31435f),
        Vector2(51.551163f, 102.52272f),
        Vector2(62.649548f, 93.85892f),
        Vector2(76.729164f, 93.85892f),
        Vector2(87.82755f, 102.52272f),
        Vector2(101.26351f, 98.31435f),
        Vector2(108.1834f, 86.05259f),
        Vector2(108.1834f, 71.973f),

    )

    return Level(connectSegmentsInLoop(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), defaultCameraOffset(worldHeight))
}

private fun makeRoundedCrossLevel(worldWidth: Float, worldHeight: Float): Level {

    val targetLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.4f

    val vertices = listOf(
        Vector2(188.5172f, 212.52927f),
        Vector2(196.79588f, 204.25058f),
        Vector2(208.27194f, 201.93259f),
        Vector2(208.27194f, 190.22476f),
        Vector2(196.79588f, 187.90677f),
        Vector2(188.5172f, 179.62808f),
        Vector2(186.1992f, 168.15202f),
        Vector2(174.49138f, 168.15202f),
        Vector2(172.17339f, 179.62808f),
        Vector2(163.8947f, 187.90677f),
        Vector2(152.41864f, 190.22476f),
        Vector2(152.41864f, 201.93259f),
        Vector2(163.8947f, 204.25058f),
        Vector2(172.17339f, 212.52927f),
        Vector2(174.49138f, 224.00533f),
        Vector2(186.1992f, 224.00533f),
        Vector2(188.5172f, 212.52927f),
    )

    return Level(connectSegmentsInLoop(makeLevelFromVertices(vertices, targetLength, worldWidth, worldHeight)), defaultCameraOffset(worldHeight))
}

/**
 * A almost-star shaped level with 16 segments. Start at the center of the screen and project outwards in a
 * straight line, then rotate 16 times around a 360 degree circle, recording one segment each time.
 * The difference with a circle is that every second one is closer to the middle to make it a star.
 *
 * You can see the original level this is modelled off here:https://youtu.be/TgZc7G8AtVU?t=108
 */
private fun makeStarLevel(worldWidth: Float, worldHeight: Float): Level {

    val approxDegreesBetween = 360f / 16
    val startingAngle = (Math.random() * 360).toFloat()
    val longLength = worldWidth.coerceAtMost(worldHeight) / 2 * 0.8f
    val shortLength = longLength * 0.7f
    val center = Vector2(worldWidth / 2, worldHeight / 2)
    val segments = (0 until 16).map { i ->

        val startLength = if (i % 2 == 0) longLength else shortLength
        val endLength = if (i % 2 == 0) shortLength else longLength
        val start = Vector2(0f, startLength).rotateDeg(startingAngle + i * -approxDegreesBetween)
        val end = Vector2(0f, endLength).rotateDeg(startingAngle + (i + 1) * -approxDegreesBetween)

        Segment(start, end).apply { offsetBy(center) }

    }

    return Level(connectSegmentsInLoop(segments), defaultCameraOffset(worldHeight))

}

private fun connectSegmentsInLine(segments: List<Segment>): List<Segment> {
    segments.forEachIndexed { i, segment ->
        segment.connect(
            if (i == segments.size - 1) null else segments[i + 1],
            if (i == 0) null else segments[i - 1],
        )
    }

    return segments
}

private fun connectSegmentsInLoop(segments: List<Segment>): List<Segment> {
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
        Direction.Clockwise -> siblingClockwise
        Direction.CounterClockwise -> siblingCounterClockwise
    }

    fun connect(siblingClockwise: Segment?, siblingCounterClockwise: Segment?) {
        this.siblingClockwise = siblingClockwise
        this.siblingCounterClockwise = siblingCounterClockwise
    }

    override fun toString() =
        "Segment[$start -> $end]"
}