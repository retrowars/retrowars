package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Asteroid(initialPosition: Vector2, val size: Size, private val velocity: Vector2, private val rotationSpeedInDegPerSec: Float): WrapsWorld {

    class Size(val radius: Float, val speed: Float, val points: Int) {

        companion object {

            val large = Size(75f, Ship.MAX_SPEED * 0.2f, 1000)
            val medium = Size(37.5f, Ship.MAX_SPEED * 0.25f, 2000)
            val small = Size(15f, Ship.MAX_SPEED * 0.3f, 3000)
            val tiny = Size(7.5f, Ship.MAX_SPEED * 0.35f, 4000)

        }

        fun nextSizeDown(): Size? = when(this) {
            large -> medium
            medium -> small
            small -> tiny
            tiny -> null
            else -> null
        }

    }

    companion object {

        /**
         * Seconds between finishing off the last asteroid on the screen, and the next asteroids respawning.
         */
        const val RESPAWN_DELAY: Float = 1.5f

        private const val ROTATION = 40f

        fun spawn(numOfAsteroids: Int, worldWidth: Float, worldHeight: Float): List<Asteroid> {

            val approxDegreesBetween = 360f / numOfAsteroids
            val startingAngle = (Math.random() * 360).toFloat()

            return (0 until numOfAsteroids).map { i ->

                // Project outward from the centre of the screen until we hit the edge, then pull back a little.
                // The idea is that we litter asteroids around the outside of the screen so they ship can
                // safely stay in the middle while they spawn.
                val ray = Vector2(0f, worldWidth / 10).rotateDeg(startingAngle + i * approxDegreesBetween)
                while (ray.x > -worldWidth / 2 && ray.x < worldWidth / 2 && ray.y > -worldHeight / 2 && ray.y < worldHeight / 2) {
                    ray.scl(1.2f)
                }
                ray.scl(0.8f)

                val rotation = ((Math.random() * 0.4 - 0.2) + 1).toFloat() * ROTATION
                val speed = ((Math.random() * 0.4 - 0.2) + 1).toFloat() * Size.large.speed
                val velocity = Vector2(0f, speed).rotateDeg((Math.random() * 360).toFloat())

                Asteroid(
                    Vector2(worldWidth / 2, worldHeight / 2).add(ray),
                    Size.large,
                    velocity,
                    rotation
                ).apply {
                    setWorldSize(worldWidth, worldHeight)
                }

            }

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
            r.circle(0f, 0f, asteroid.size.radius, 7)
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
        val nextSize = size.nextSizeDown() ?: return emptyList()

        return (0 until 2).map {
            // Offset the location of the new asteroids slightly from the centre
            val newPositionOffsetAngle = (Math.random() * 360).toFloat()
            val newPositionOffset = Vector2(0f, nextSize.radius).rotateDeg(newPositionOffsetAngle)
            val newPosition = position
                .cpy()
                .mulAdd(newPositionOffset, 1f)

            // The new asteroid will have a similar velocity to the current (though greater), except
            // we will randomly shift the direction from -45 degrees to +45 degrees.
            val newVelocityOffsetFromCurrent = (Math.random() * 90 - 45).toFloat()
            val speed = ((Math.random() * 0.4 - 0.2) + 1).toFloat() * nextSize.speed
            val newVelocity = velocity
                .cpy()
                .rotateDeg(newVelocityOffsetFromCurrent)
                .nor()
                .scl(speed)

            // We could also increase the speed of smaller items, but it seems that having smaller
            // asteroids travel the same speed as larger ones result in a visual perception that
            // they are moving faster for some reason, so right now will leave the speed as is.

            val rotation = ((Math.random() * 0.4 - 0.2) + 1).toFloat() * ROTATION

            Asteroid(newPosition, nextSize, newVelocity, rotation)
        }
    }

    fun isColliding(entity: HasBoundingSphere): Boolean {
        val distance2 = entity.getPosition().dst2(this.position)
        val collideDistance = (size.radius + entity.getRadius())

        return distance2 < collideDistance * collideDistance
    }

    override fun isFullyPastRight() = isFullyPastRight(position.x - size.radius)
    override fun isPartiallyPastRight() = isPartiallyPastRight(position.x + size.radius)

    override fun isFullyPastTop() = isFullyPastTop(position.y - size.radius)
    override fun isPartiallyPastTop() = isPartiallyPastTop(position.y + size.radius)

    override fun isFullyPastLeft() = isFullyPastLeft(position.x + size.radius)
    override fun isPartiallyPastLeft() = isPartiallyPastLeft(position.x - size.radius)

    override fun isFullyPastBottom() = isFullyPastBottom(position.y + size.radius)
    override fun isPartiallyPastBottom() = isPartiallyPastBottom(position.y - size.radius)

}