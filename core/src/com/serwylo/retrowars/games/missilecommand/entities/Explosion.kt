package com.serwylo.retrowars.games.missilecommand.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.asteroids.entities.HasBoundingSphere

class Explosion(private val position: Vector2) {

    companion object {

        private const val MAX_RADIUS = 15f
        private const val DURATION = 2f
        private const val SIZE_PER_SECOND = MAX_RADIUS / DURATION

        fun renderBulk(camera: Camera, r: ShapeRenderer, explosions: List<Explosion>) {
            r.projectionMatrix = camera.combined
            r.color = Color.WHITE
            r.begin(ShapeRenderer.ShapeType.Filled)
            explosions.forEach {
                r.circle(it.position.x, it.position.y, it.radius, 16)
            }
            r.end()
        }

    }

    private var radius = 0f

    fun update(delta: Float) {
        radius += SIZE_PER_SECOND * delta
    }

    fun hasReachedMaxSize() = radius >= MAX_RADIUS

}