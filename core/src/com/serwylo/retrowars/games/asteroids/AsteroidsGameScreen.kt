package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.asteroids.entities.Asteroid
import com.serwylo.retrowars.games.asteroids.entities.Bullet
import com.serwylo.retrowars.games.asteroids.entities.HasBoundingSphere
import com.serwylo.retrowars.games.asteroids.entities.Ship
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.ui.GameViewport
import com.serwylo.retrowars.ui.HUD

class AsteroidsGameScreen(private val game: RetrowarsGame) : Screen {

    companion object {
        const val MIN_WORLD_WIDTH = 400f
        const val MIN_WORLD_HEIGHT = 400f
        const val TAG = "AsteroidsGameScreen"
    }

    private val camera = OrthographicCamera()
    private val viewport = GameViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, camera)

    private val state: AsteroidsGameState

    private val hud: HUD

    private val client = RetrowarsClient.get()

    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        viewport.apply(true)

        state = AsteroidsGameState(viewport.worldWidth, viewport.worldHeight)

        state.ship.onShoot {
            it.setWorldSize(camera.viewportWidth, camera.viewportHeight)
            state.bullets.add(it)
        }

        hud = HUD(game.uiAssets)
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
        updateEntities(delta)

        Gdx.graphics.gL20.glClearColor(0f, 0f, 0f, 1f)
        Gdx.graphics.gL20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.renderIn {
            renderEntities()
        }

        hud.render(state.score, delta)

    }

    private fun updateEntities(delta: Float) {

        if (state.isShipAlive()) {

            // If we have not died (i.e. we are not in the mandatory waiting period for respawning the ship).
            state.ship.update(delta)

        } else if (state.isShipReadyToRespawn()) {

            // If we have died, waited the minimum amount of time, and are now ready to respawn...
            respawnShipIfSafe()

        }

        state.bullets.forEach { it.update(delta) }
        state.bullets.removeAll { it.isExpired() }

        state.asteroids.forEach { it.update(delta) }

        checkCollisions()

        if (state.asteroids.size == 0 && state.nextAsteroidRespawnTime < 0f) {
            queueAsteroidsRespawn()
        } else if (state.areAsteroidsReadyToRespawn()) {
            respawnAsteroids()
        }

    }

    object SafeRespawnArea : HasBoundingSphere {

        val screenCentre = Vector2()

        override fun getPosition() = screenCentre
        override fun getRadius() = Ship.HEIGHT * 3

    }

    /**
     * Check a bounding box in the middle of the screen, and if there are no asteroids there,
     * then respawn the ship there and reset the respawn counter.
     */
    private fun respawnShipIfSafe() {

        val asteroidsInWay = state.asteroids.any { asteroid ->
            asteroid.isColliding(SafeRespawnArea)
        }

        if (asteroidsInWay) {
            return
        }

        state.ship.respawnInCentre()
        state.nextShipRespawnTime = -1f

    }

    // TODO: Doesn't correctly check for collisions on the wrapped side of the world.
    private fun checkCollisions() {
        val asteroidsToBreak = mutableListOf<Asteroid>()

        state.asteroids.forEach { asteroid ->

            if (state.isShipAlive() && asteroid.isColliding(state.ship)) {

                asteroidsToBreak.add(asteroid)

                shipHit()

            } else {

                val bullet = state.bullets.firstOrNull { asteroid.isColliding(it) }

                if (bullet != null) {
                    asteroidsToBreak.add(asteroid)
                    state.bullets.remove(bullet)
                    state.score += asteroid.size.points
                    client?.updateScore(state.score)
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

    private fun shipHit() {
        state.numLives--

        if (state.numLives <= 0) {
            if (client == null) {
                // TODO: Record high score, show end of game screen.
                Gdx.app.log(TAG, "No more lives left... Loading game select menu (no network game)")
                game.showGameSelectMenu()
            } else {
                Gdx.app.log(TAG, "No more lives left... Off to the end-game lobby.")
                client.chagneStatus(Player.Status.dead)
                game.showEndMultiplayerGame()
            }
        } else {
            state.nextShipRespawnTime = state.timer + AsteroidsGameState.SHIP_RESPAWN_DELAY
        }
    }

    private fun queueAsteroidsRespawn() {
        state.nextAsteroidRespawnTime = state.timer + Asteroid.RESPAWN_DELAY
    }

    /**
     * Add a new collection of large asteroids around the edge of the screen. There will be one
     * more than last time we did this. We will then reset the respawning status.
     */
    private fun respawnAsteroids() {
        val numToRespawn = ++state.currentNumAsteroids
        state.nextAsteroidRespawnTime = -1f
        state.asteroids.addAll(Asteroid.spawn(numToRespawn, viewport.worldWidth, viewport.worldHeight))
    }

    private fun renderEntities() {
        val r = game.uiAssets.shapeRenderer

        // Make the ship disappear when respawning. It will then reappear in the future when ready to
        // go again.
        if (state.isShipAlive()) {
            state.ship.render(camera, r)
        }

        Bullet.renderBulk(camera, r, state.bullets)
        Asteroid.renderBulk(camera, r, state.asteroids)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        hud.resize(width, height)
        state.ship.setWorldSize(camera.viewportWidth, camera.viewportHeight)
        state.bullets.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }
        state.asteroids.forEach { it.setWorldSize(camera.viewportWidth, camera.viewportHeight) }
        SafeRespawnArea.screenCentre.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f)
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
