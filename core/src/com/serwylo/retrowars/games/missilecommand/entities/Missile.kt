package com.serwylo.retrowars.games.missilecommand.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Missile(private val start: Vector2, val target: Vector2) {

    companion object {

        private const val SIZE = 2f
        private const val SPEED = 100f

        private val COLOUR_TRAIL = Color(0.2f, 1f, 0.2f, 0.5f)
        private val COLOUR_MISSILE = Color(0.2f, 1f, 0.2f, 1f)

        fun renderBulk(camera: Camera, r: ShapeRenderer, missiles: List<Missile>) {
            r.projectionMatrix = camera.combined
            r.color = Color.WHITE
            r.begin(ShapeRenderer.ShapeType.Line)
            missiles.forEach {
                r.color = COLOUR_TRAIL
                r.line(it.start, it.position)

                r.color = COLOUR_MISSILE
                r.rect(it.position.x - SIZE / 2, it.position.y - SIZE / 2, SIZE, SIZE)
            }
            r.end()
        }

    }

    private val position = start.cpy()
    private val velocity = target.cpy().sub(start).nor().scl(SPEED)

    fun update(delta: Float) {
        position.mulAdd(velocity, delta)
    }

    fun hasReachedDestination() = position.y >= target.y

}