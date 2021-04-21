package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Bullet(initialPosition: Vector2 = Vector2(0f, 0f), directionInDegrees: Float): WrapsWorld {

    companion object {

        const val WIDTH = 1f
        const val HEIGHT = 1f

        // Shouldn't be able to catch our own bullets, so always go faster than the fastest 
        const val SPEED = Ship.MAX_SPEED * 1.2f

        /**
         * Time in seconds that this bullet should live for. After this it will just be removed.
         */
        const val MAX_AGE = 0.75f

        fun renderBulk(camera: Camera, r: ShapeRenderer, bullets: List<Bullet>) {
            r.projectionMatrix = camera.combined
            r.color = Color.WHITE
            r.begin(ShapeRenderer.ShapeType.Line)
            bullets.forEach {
                r.rect(it.position.x, it.position.y, WIDTH, HEIGHT)
            }
            r.end()
        }

    }

    override var worldWidth = 0f
    override var worldHeight = 0f

    private val position = initialPosition.cpy()
    private val velocity = Vector2(0f, SPEED).rotateDeg(directionInDegrees)
    private var age = 0f

    fun update(delta: Float) {
        age += delta
        position.mulAdd(velocity, delta)
        maybeWrapPosition(position)
    }

    fun isExpired() = age > MAX_AGE

    override fun isFullyPastRight() = isFullyPastRight(position.x)
    override fun isPartiallyPastRight() = isPartiallyPastRight(position.x)

    override fun isFullyPastTop() = isFullyPastTop(position.y)
    override fun isPartiallyPastTop() = isPartiallyPastTop(position.y)

    override fun isFullyPastLeft() = isFullyPastLeft(position.x)
    override fun isPartiallyPastLeft() = isPartiallyPastLeft(position.x)

    override fun isFullyPastBottom() = isFullyPastBottom(position.y)
    override fun isPartiallyPastBottom() = isPartiallyPastBottom(position.y)

}