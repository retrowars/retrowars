package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.tetris.ButtonState
import java.util.*

class TempestGameState(private val worldWidth: Float, private val worldHeight: Float) {

    companion object {
        const val LEVEL_DEPTH = 150f
        const val BULLET_SPEED = LEVEL_DEPTH / 0.5f // Take 0.5 seconds to traverse the whole screen.
    }
    val bullets = LinkedList<Bullet>()
    val level: Level = makeThirdLevel(worldWidth, worldHeight)

    var moveCounterClockwise = ButtonState.Unpressed
    var moveClockwise = ButtonState.Unpressed
    var fire = ButtonState.Unpressed

    var playerSegment = level.segments[0]
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
