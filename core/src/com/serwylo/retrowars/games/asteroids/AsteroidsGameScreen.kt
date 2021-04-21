package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.asteroids.entities.Asteroid
import com.serwylo.retrowars.games.asteroids.entities.Bullet
import com.serwylo.retrowars.games.asteroids.entities.Ship
import com.gmail.blueboxware.libgdxplugin.annotations.GDXAssets
import java.util.*

class AsteroidsGameScreen(private val game: RetrowarsGame) : Screen {

    companion object {
        const val MIN_WORLD_WIDTH = 400f
        const val MIN_WORLD_HEIGHT = 400f
    }

    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, camera)
    private val ship: Ship
    private val bullets = LinkedList<Bullet>()
    private val asteroids = mutableListOf<Asteroid>()

    init {
        viewport.apply(true)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        ship = Ship(Vector2(viewport.worldWidth / 2, viewport.worldHeight / 2))
        ship.setWorldSize(viewport.worldWidth, viewport.worldHeight)
        ship.onShoot {
            it.setWorldSize(camera.viewportWidth, camera.viewportHeight)
            bullets.add(it)
        }

        repeat(3) {
            asteroids.add(Asteroid.spawn(Asteroid.Size.large, viewport.worldWidth, viewport.worldHeight))
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = object: InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT -> ship.left = true
                    Input.Keys.A -> ship.left = true
                    Input.Keys.RIGHT -> ship.right = true
                    Input.Keys.D -> ship.right = true
                    Input.Keys.UP -> ship.thrust = true
                    Input.Keys.W -> ship.thrust = true
                    Input.Keys.SPACE -> ship.shooting = true
                    else -> return false
                }

                return true
            }

            override fun keyUp(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT -> ship.left = false
                    Input.Keys.A -> ship.left = false
                    Input.Keys.RIGHT -> ship.right = false
                    Input.Keys.D -> ship.right = false
                    Input.Keys.UP -> ship.thrust = false
                    Input.Keys.W -> ship.thrust = false
                    Input.Keys.SPACE -> ship.shooting = false
                    else -> return false
                }

                return true
            }
        }
    }

    override fun render(delta: Float) {

        Gdx.graphics.gL20.glClearColor(0f, 0f, 0f, 1f)
        Gdx.graphics.gL20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined
        r.begin(ShapeRenderer.ShapeType.Line)
        r.rect(5f, 5f, camera.viewportWidth - 10, camera.viewportHeight - 10)
        r.end()

        updateEntities(delta)
        renderEntities()

    }

    private fun updateEntities(delta: Float) {

        ship.update(delta)

        bullets.forEach { it.update(delta) }
        bullets.removeAll { it.isExpired() }

        asteroids.forEach { it.update(delta) }

        checkCollisions()

    }

    private fun checkCollisions() {
        val asteroidsToBreak = mutableListOf<Asteroid>()

        asteroids.forEach { asteroid ->

            if (asteroid.isColliding(ship)) {

                asteroidsToBreak.add(asteroid)

                // TODO: Lose health

            } else {

                val bullet = bullets.firstOrNull { asteroid.isColliding(it) }

                if (bullet != null) {
                    asteroidsToBreak.add(asteroid)
                    bullets.remove(bullet)
                }

            }

        }

        asteroidsToBreak.forEach { toBreak ->

            val newAsteroids = toBreak.split()
            newAsteroids.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }

            asteroids.remove(toBreak)
            asteroids.addAll(newAsteroids)

        }
    }

    private fun renderEntities() {
        val r = game.uiAssets.shapeRenderer

        ship.render(camera, r)
        Bullet.renderBulk(camera, r, bullets)
        Asteroid.renderBulk(camera, r, asteroids)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        ship.setWorldSize(camera.viewportWidth, camera.viewportHeight)
        bullets.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }
        asteroids.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
    }

}
