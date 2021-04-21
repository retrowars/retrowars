package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Asteroid(initialPosition: Vector2, private val size: Float, private val velocity: Vector2, private val rotationSpeedInDegPerSec: Float): WrapsWorld {

    object Size {
        const val large = 150f
        const val medium = 75f
        const val small = 30f
        const val tiny = 15f

        fun nextSizeDown(currentSize: Float): Float? = when(currentSize) {
            large -> medium
            medium -> small
            small -> tiny
            tiny -> null
            else -> null
        }
    }

    companion object {

        // Shouldn't be able to catch our own bullets, so always go faster than the fastest 
        private const val MIN_SPEED = Ship.MAX_SPEED * 0.1
        private const val MAX_SPEED = Ship.MAX_SPEED * 0.25

        private const val MIN_ROTATION = 20f
        private const val MAX_ROTATION = 100f

        fun spawn(size: Float, worldWidth: Float, worldHeight: Float): Asteroid {
            val pos = Vector2((Math.random() * worldWidth).toFloat(), (Math.random() * worldHeight).toFloat())
            val speed = (Math.random() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED).toFloat()
            val velocity = Vector2(0f, speed).rotateDeg((Math.random() * 360).toFloat())
            val rotation = (Math.random() * (MAX_ROTATION - MIN_ROTATION) + MIN_ROTATION).toFloat()

            return Asteroid(pos, size, velocity, rotation).apply { setWorldSize(worldWidth, worldHeight) }
        }

        fun renderBulk(camera: Camera, r: ShapeRenderer, asteroids: List<Asteroid>) {
            r.projectionMatrix = camera.combined
            r.color = Color.WHITE
            r.begin(ShapeRenderer.ShapeType.Line)
            asteroids.forEach {
                renderInBatch(r, it, it.position)

                if (it.isPartiallyPastBottom()) {
                    renderInBatch(r, it, it.position.cpy().add(0f, camera.viewportHeight))
                } else if (it.isPartiallyPastTop()) {
                    renderInBatch(r, it, it.position.cpy().sub(0f, camera.viewportHeight))
                }

                if (it.isPartiallyPastLeft()) {
                    renderInBatch(r, it, it.position.cpy().add(camera.viewportWidth, 0f))
                } else if (it.isPartiallyPastRight()) {
                    renderInBatch(r, it, it.position.cpy().sub(camera.viewportWidth, 0f))
                }
            }
            r.end()
            r.identity()
        }

        private fun renderInBatch(r: ShapeRenderer, asteroid: Asteroid, position: Vector2) {
            r.identity()
            r.translate(position.x, position.y, 0f)
            r.rotate(0f, 0f, 1f, asteroid.rotationInDegrees)
            r.circle(0f, 0f, asteroid.size / 2, 7)
        }

    }

    override var worldWidth = 0f
    override var worldHeight = 0f

    private var rotationInDegrees = 0f

    private val position = initialPosition.cpy()

    fun update(delta: Float) {
        rotationInDegrees += (rotationSpeedInDegPerSec * delta)
        position.mulAdd(velocity, delta)
        maybeWrapPosition(position)
    }

    /**
     * When this asteroid is hit, it will split into smaller asteroids in the same vicinity (or
     * if it is already very small, return an empty list).
     */
    fun split(): List<Asteroid> {
        val nextSize = Size.nextSizeDown(size) ?: return emptyList()

        return (0 until 2).map {
            // Offset the location of the new asteroids slightly from the centre
            val newPositionOffsetAngle = (Math.random() * 360).toFloat()
            val newPositionOffset = Vector2(0f, nextSize / 2).rotateDeg(newPositionOffsetAngle)
            val newPosition = position
                .cpy()
                .mulAdd(newPositionOffset, 1f)

            // The new asteroid will have a similar velocity to the current (though greater), except
            // we will randomly shift the direction from -45 degrees to +45 degrees.
            val newVelocityOffsetFromCurrent = (Math.random() * 90 - 45).toFloat()
            val newVelocity = velocity
                .cpy()
                .rotateDeg(newVelocityOffsetFromCurrent)

            // We could also increase the speed of smaller items, but it seems that having smaller
            // asteroids travel the same speed as larger ones result in a visual perception that
            // they are moving faster for some reason, so right now will leave the speed as is.

            val rotation = (Math.random() * (MAX_ROTATION - MIN_ROTATION) + MIN_ROTATION).toFloat()

            Asteroid(newPosition, nextSize, newVelocity, rotation)
        }
    }

    fun isColliding(entity: HasBoundingSphere): Boolean {
        val distance2 = entity.getPosition().dst2(this.position)
        val collideDistance = (size / 2 + entity.getRadius())

        return distance2 < collideDistance * collideDistance
    }

    override fun isFullyPastRight() = isFullyPastRight(position.x - size / 2)
    override fun isPartiallyPastRight() = isPartiallyPastRight(position.x + size / 2)

    override fun isFullyPastTop() = isFullyPastTop(position.y - size / 2)
    override fun isPartiallyPastTop() = isPartiallyPastTop(position.y + size / 2)

    override fun isFullyPastLeft() = isFullyPastLeft(position.x + size / 2)
    override fun isPartiallyPastLeft() = isPartiallyPastLeft(position.x - size / 2)

    override fun isFullyPastBottom() = isFullyPastBottom(position.y + size / 2)
    override fun isPartiallyPastBottom() = isPartiallyPastBottom(position.y - size / 2)

}