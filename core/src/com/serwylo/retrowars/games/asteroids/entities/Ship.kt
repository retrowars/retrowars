package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Ship(initialPosition: Vector2 = Vector2(0f, 0f)): WrapsWorld, HasBoundingSphere {

    companion object {

        /**
         * If holding down left or right, take this many seconds to do a full rotation.
         */
        const val ROTATION_SPEED = 360 / 1.5f

        const val WIDTH = 8f
        const val HEIGHT = 16f

        const val ACCEL = 200f

        const val MAX_SPEED = 250f
        const val MAX_SPEED_2 = MAX_SPEED * MAX_SPEED

    }

    override var worldWidth = 0f
    override var worldHeight = 0f

    var left = false
    var right = false

    /**
     * Record whether or not it was thrusting the frame prior, so that we can start + stop the sound
     * effects accordingly.
     */
    var wasThrusting = false
    var thrust = false

    var shooting = false
        set(value) {
            if (field != value) {
                field = value
                isReloading = false
            }
        }

    private var isReloading = false

    private val position = initialPosition.cpy()
    val velocity = Vector2()
    val acceleration = Vector2()
    var rotationInDegrees = 0f

    private var onShootListener: ((Bullet) -> Unit)? = null

    fun render(camera: Camera, r: ShapeRenderer) {

        renderShipAt(camera, r, position)

        if (isPartiallyPastBottom()) {
            renderShipAt(camera, r, position.cpy().add(0f, worldHeight))
        } else if (isPartiallyPastTop()) {
            renderShipAt(camera, r, position.cpy().sub(0f, worldHeight))
        }

        if (isPartiallyPastLeft()) {
            renderShipAt(camera, r, position.cpy().add(worldWidth, 0f))
        } else if (isPartiallyPastRight()) {
            renderShipAt(camera, r, position.cpy().sub(worldWidth, 0f))
        }
    }

    //
    //     E
    //    / \
    //   /   \    <- Main ship
    //  /     \
    // A--B-C--D
    //    \ /     <- Fire below the ship when accelerating.
    //     F
    //
    // Don't want to have to perform the math for every render, so lets just calculate once.
    //
    private val vertexTop = HEIGHT / 2f
    private val vertexBottom = -HEIGHT / 2f
    private val vertexLeft = -WIDTH / 2f
    private val vertexRight = WIDTH / 2f
    private val vertexThrustBottom = vertexBottom - (HEIGHT / 4f)
    private val vertexThrustLeft = vertexLeft + (WIDTH / 3f)
    private val vertexThrustRight = vertexRight - (WIDTH / 3f)

    private fun renderShipAt(camera: Camera, r: ShapeRenderer, position: Vector2) {
        r.projectionMatrix = camera.combined
        r.begin(ShapeRenderer.ShapeType.Line)
        r.identity()
        r.translate(position.x, position.y, 0f)
        r.rotate(0f, 0f, 1f, rotationInDegrees - 90)
        r.color = Color.WHITE

        r.line(vertexLeft, vertexBottom, vertexRight, vertexBottom) // A -> D
        r.line(vertexRight, vertexBottom, 0f, vertexTop) // D -> E
        r.line(0f, vertexTop, vertexLeft, vertexBottom) // E -> A

        if (thrust) {
            r.line(vertexThrustLeft, vertexBottom, 0f, vertexThrustBottom) // B -> F
            r.line(0f, vertexThrustBottom, vertexThrustRight, vertexBottom) // C -> F
        }

        r.end()
        r.identity()
    }

    fun update(delta: Float) {

        if (shooting) {
            if (!isReloading) {
                onShootListener?.invoke(Bullet(position.cpy(), rotationInDegrees - 90))
                isReloading = true
            }
        }

        if (left && !right) {
            rotationInDegrees += ROTATION_SPEED * delta
        } else if (right && !left) {
            rotationInDegrees -= ROTATION_SPEED * delta
        }

        if (thrust) {
            acceleration.set(ACCEL, 0f)
            acceleration.setAngleDeg(rotationInDegrees)
            velocity.mulAdd(acceleration, delta)

            if (velocity.len2() > MAX_SPEED_2) {
                val normal = velocity.nor()
                velocity.nor().mulAdd(normal, MAX_SPEED)
            }
        }

        position.mulAdd(velocity, delta)
        maybeWrapPosition(position)
    }

    override fun isFullyPastRight() = isFullyPastRight(position.x - WIDTH / 2f)
    override fun isPartiallyPastRight() = isPartiallyPastRight(position.x + WIDTH / 2f)

    override fun isFullyPastTop() = isFullyPastTop(position.y - HEIGHT / 2f)
    override fun isPartiallyPastTop() = isPartiallyPastTop(position.y + HEIGHT / 2f)

    override fun isFullyPastLeft() = isFullyPastLeft(position.x + WIDTH / 2f)
    override fun isPartiallyPastLeft() = isPartiallyPastLeft(position.x - WIDTH / 2f)

    override fun isFullyPastBottom() = isFullyPastBottom(position.y + HEIGHT / 2f)
    override fun isPartiallyPastBottom() = isPartiallyPastBottom(position.y - HEIGHT / 2f)

    fun onShoot(listener: (Bullet) -> Unit) {
        onShootListener = listener
    }

    override fun getPosition(): Vector2 = position

    override fun getRadius() = HEIGHT / 2

    fun respawnInCentre() {
        position.set(worldWidth / 2f, worldHeight / 2f)
        velocity.set(0f, 0f)
        acceleration.set(0f, 0f)
        rotationInDegrees = 0f
    }

}