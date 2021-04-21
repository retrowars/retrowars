package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Ship(initialPosition: Vector2 = Vector2(0f, 0f)): WrapsWorld {

    companion object {

        /**
         * If holding down left or right, take this many seconds to do a full rotation.
         */
        const val ROTATION_SPEED = 360 / 1.5f

        const val WIDTH = 8f
        const val HEIGHT = 16f

        const val ACCEL = 200f

        const val MAX_SPEED = 400f
        const val MAX_SPEED_2 = MAX_SPEED * MAX_SPEED

    }

    override var worldWidth = 0f
    override var worldHeight = 0f

    var left = false
    var right = false
    var thrust = false
    var shooting = false
        set(value) {
            field = value
            isReloading = false
        }

    private var isReloading = false

    private val position = initialPosition.cpy()
    private val velocity = Vector2()
    private val acceleration = Vector2()
    private var rotationInDegrees = 0f

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

    private fun renderShipAt(camera: Camera, r: ShapeRenderer, position: Vector2) {
        r.projectionMatrix = camera.combined
        r.begin(ShapeRenderer.ShapeType.Line)
        r.identity()
        r.translate(position.x, position.y, 0f)
        r.rotate(0f, 0f, 1f, rotationInDegrees - 90)
        r.color = Color.WHITE

        //
        //    C
        //   / \
        //  /   \
        // A-----B
        //
        r.line(-WIDTH / 2f, -HEIGHT / 2f, WIDTH / 2f, -HEIGHT / 2f) // A -> B
        r.line(WIDTH / 2f, -HEIGHT / 2f, 0f, HEIGHT / 2f) // B -> C
        r.line(0f, HEIGHT / 2f, -WIDTH / 2f, -HEIGHT / 2f) // C -> A

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

}