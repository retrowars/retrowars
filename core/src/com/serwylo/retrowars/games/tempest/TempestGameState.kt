package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.math.Vector2

class TempestGameState(private val worldWidth: Float, private val worldHeight: Float) {
    val level: Level = makeFirstLevel(worldWidth, worldHeight)
}

data class Level(
    val segments: List<Segment>,
    val isLoop: Boolean,
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

        Segment(start.add(center), end.add(center))

    }

    return Level(segments, true)

}

data class Segment(
    val start: Vector2,
    val end: Vector2,
)