package com.serwylo.retrowars.games.missilecommand.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.asteroids.entities.HasBoundingSphere

class Missile(private val type: Type, private val start: Vector2, val target: Vector2) {

    class Type(val speed: Float, val missileColour: Color, val trailColour: Color) {
        companion object {
            val friendly = Type(
                200f,
                Color(0.2f, 1f, 0.2f, 1f),
                Color(0.2f, 1f, 0.2f, 0.5f)
            )

            val enemy = Type(
                25f,
                Color(1f, 0.2f, 0.2f, 1f),
                Color(1f, 0.2f, 0.2f, 0.5f)
            )
        }
    }

    companion object {

        private const val SIZE = 2f

        fun renderBulk(camera: Camera, r: ShapeRenderer, missiles: List<Missile>) {
            r.projectionMatrix = camera.combined
            r.color = Color.WHITE
            r.begin(ShapeRenderer.ShapeType.Line)
            missiles.forEach {
                r.color = it.type.trailColour
                r.line(it.start, it.position)

                r.color = it.type.missileColour
                r.rect(it.position.x - SIZE / 2, it.position.y - SIZE / 2, SIZE, SIZE)
            }
            r.end()
        }

    }

    private val position = start.cpy()
    private val velocity = target.cpy().sub(start).nor().scl(type.speed)

    fun update(delta: Float) {
        position.mulAdd(velocity, delta)
    }

    fun hasReachedDestination() = position.y >= target.y

    fun isColliding(entity: HasBoundingSphere): Boolean {
        val distance2 = entity.getPosition().dst2(this.position)
        val collideDistance = (SIZE / 2 + entity.getRadius())

        return distance2 < collideDistance * collideDistance
    }

}