package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.asteroids.entities.Asteroid
import com.serwylo.retrowars.games.asteroids.entities.Bullet

class AsteroidsGameScreen(private val game: RetrowarsGame) : Screen {

    companion object {
        const val MIN_WORLD_WIDTH = 400f
        const val MIN_WORLD_HEIGHT = 400f
        const val TAG = "AsteroidsGameScreen"
    }

    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, camera)

    private val state: AsteroidsGameState

    private val hud: HUD

    init {
        viewport.apply(true)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        state = AsteroidsGameState(viewport.worldWidth, viewport.worldHeight)

        state.ship.onShoot {
            it.setWorldSize(camera.viewportWidth, camera.viewportHeight)
            state.bullets.add(it)
        }

        hud = HUD(state, game.uiAssets)
    }

    override fun show() {
        Gdx.input.inputProcessor = object: InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT -> state.ship.left = true
                    Input.Keys.A -> state.ship.left = true
                    Input.Keys.RIGHT -> state.ship.right = true
                    Input.Keys.D -> state.ship.right = true
                    Input.Keys.UP -> state.ship.thrust = true
                    Input.Keys.W -> state.ship.thrust = true
                    Input.Keys.SPACE -> state.ship.shooting = true
                    else -> return false
                }

                return true
            }

            override fun keyUp(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.LEFT -> state.ship.left = false
                    Input.Keys.A -> state.ship.left = false
                    Input.Keys.RIGHT -> state.ship.right = false
                    Input.Keys.D -> state.ship.right = false
                    Input.Keys.UP -> state.ship.thrust = false
                    Input.Keys.W -> state.ship.thrust = false
                    Input.Keys.SPACE -> state.ship.shooting = false
                    else -> return false
                }

                return true
            }
        }
    }

    override fun render(delta: Float) {

        state.timer += delta

        Gdx.graphics.gL20.glClearColor(0f, 0f, 0f, 1f)
        Gdx.graphics.gL20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        updateEntities(delta)
        renderEntities()

        hud.render(delta)

    }

    private fun updateEntities(delta: Float) {

        state.ship.update(delta)

        state.bullets.forEach { it.update(delta) }
        state.bullets.removeAll { it.isExpired() }

        state.asteroids.forEach { it.update(delta) }

        checkCollisions()

        if (state.asteroids.size == 0 && state.nextRespawnTime < 0f) {
            queueAsteroidsRespawn()
        }

        respawnAsteroids()

    }

    private fun checkCollisions() {
        val asteroidsToBreak = mutableListOf<Asteroid>()

        state.asteroids.forEach { asteroid ->

            if (asteroid.isColliding(state.ship)) {

                asteroidsToBreak.add(asteroid)
                state.numLives--

            } else {

                val bullet = state.bullets.firstOrNull { asteroid.isColliding(it) }

                if (bullet != null) {
                    asteroidsToBreak.add(asteroid)
                    state.bullets.remove(bullet)
                }

            }

        }

        asteroidsToBreak.forEach { toBreak ->

            val newAsteroids = toBreak.split()
            newAsteroids.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }

            state.asteroids.remove(toBreak)
            state.asteroids.addAll(newAsteroids)

        }
    }

    private fun queueAsteroidsRespawn() {
        Gdx.app.log(TAG, "Queueing a respawn of more asteroids in ${Asteroid.RESPAWN_DELAY}s.")
        state.nextRespawnTime = state.timer + Asteroid.RESPAWN_DELAY
    }

    private fun respawnAsteroids() {
        if (state.nextRespawnTime > 0f && state.nextRespawnTime < state.timer) {
            val numToRespawn = ++state.currentNumAsteroids
            state.nextRespawnTime = -1f
            state.asteroids.addAll(Asteroid.spawn(numToRespawn, viewport.worldWidth, viewport.worldHeight))

            Gdx.app.log(TAG, "Respawned $numToRespawn asteroids.")
        }
    }

    private fun renderEntities() {
        val r = game.uiAssets.shapeRenderer

        state.ship.render(camera, r)
        Bullet.renderBulk(camera, r, state.bullets)
        Asteroid.renderBulk(camera, r, state.asteroids)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        state.ship.setWorldSize(camera.viewportWidth, camera.viewportHeight)
        state.bullets.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }
        state.asteroids.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }
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
