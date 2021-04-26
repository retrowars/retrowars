package com.serwylo.retrowars.games.missilecommand.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class City(val position: Vector2) {

    var health = INITIAL_HEALTH

    companion object {
        const val WIDTH = 20f
        const val HEIGHT = 15f
        const val INITIAL_HEALTH = 1

        val colours = listOf(
            Color.GRAY,
            Color.GREEN
        )
    }

    fun render(camera: Camera, r: ShapeRenderer) {
        r.identity()
        r.projectionMatrix = camera.combined
        r.translate(position.x, position.y, 0f)

        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = colours[health.coerceAtLeast(0)]
        r.rect(-WIDTH / 2f, 0f, WIDTH, HEIGHT)
        r.end()

        r.identity()
    }

}