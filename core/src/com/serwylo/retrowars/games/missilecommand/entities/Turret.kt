package com.serwylo.retrowars.games.missilecommand.entities

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class Turret(val position: Vector2) {

    companion object {
        const val WIDTH = 15f
        const val HEIGHT = 30f

        const val AMMO_RADIUS = 2f
        const val INITIAL_AMMUNITION = 8
    }

    var ammunition = INITIAL_AMMUNITION

    fun render(camera: Camera, r: ShapeRenderer) {

        r.identity()
        r.projectionMatrix = camera.combined
        r.translate(position.x, position.y, 0f)

        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = Color.BLUE

        r.rect(-WIDTH / 2f, 0f, WIDTH, HEIGHT)

        val perRow = 2
        val spacing = WIDTH / (perRow + 1)
        for (i in 0 until ammunition) {
            val row = i / perRow
            val col = i % perRow

            r.circle(
                -WIDTH / 2 + (col + 1) * spacing,
                (row + 1) * spacing,
                AMMO_RADIUS
            )
        }

        r.end()

        r.identity()

    }

}