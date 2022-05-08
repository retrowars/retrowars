package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.asteroids.entities.Asteroid
import com.serwylo.retrowars.games.asteroids.entities.Bullet
import com.serwylo.retrowars.games.asteroids.entities.HasBoundingSphere
import com.serwylo.retrowars.games.asteroids.entities.Ship
import com.serwylo.retrowars.input.AsteroidsSoftController

class AsteroidsGameScreen(game: RetrowarsGame) : GameScreen(game, Games.asteroids, 400f, 400f) {

    companion object {
        @Suppress("unused")
        const val TAG = "AsteroidsGameScreen"
    }

    private val state = AsteroidsGameState(viewport.worldWidth, viewport.worldHeight)

    private val lifeContainer = HorizontalGroup().apply { space(UI_SPACE) }

    private val sounds = AsteroidsSoundLibrary()

    init {

        state.ship.onShoot {
            it.setWorldSize(viewport.worldWidth, viewport.worldHeight)
            state.bullets.add(it)
            sounds.fire()
        }

        addGameScoreToHUD(lifeContainer)

    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
        state.timer += delta

        controller!!.update(delta)

        if (getState() == State.Playing) {
            state.ship.left = controller.trigger(AsteroidsSoftController.Buttons.LEFT)
            state.ship.right = controller.trigger(AsteroidsSoftController.Buttons.RIGHT)
            state.ship.shooting = controller.trigger(AsteroidsSoftController.Buttons.FIRE)

            state.ship.wasThrusting = state.ship.thrust
            state.ship.thrust = controller.trigger(AsteroidsSoftController.Buttons.THRUST)
        }

        updateEntities(delta)

        if (getState() == State.Playing && state.numLives <= 0) {
            sounds.stopThrust()
            endGame()
        }
    }

    private fun updateEntities(delta: Float) {

        if (getState() == State.Playing) {

            if (state.isShipAlive()) {

                // If we have not died (i.e. we are not in the mandatory waiting period for respawning the ship).
                state.ship.update(delta)

                if (state.ship.wasThrusting != state.ship.thrust) {
                    if (state.ship.thrust) {
                        sounds.startThrust()
                    } else {
                        sounds.stopThrust()
                    }
                }

            } else {
                sounds.stopThrust()
                if (state.isShipReadyToRespawn()) {
                    // If we have died, waited the minimum amount of time, and are now ready to respawn...
                    respawnShipIfSafe()
                }
            }

        }

        state.bullets.forEach { it.update(delta) }
        state.bullets.removeAll { it.isExpired() }

        val iterator = state.asteroids.iterator()
        while (iterator.hasNext()) {
            iterator.next().update(delta)
        }

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

        val iterator = state.asteroids.iterator()
        while (iterator.hasNext()) {
            val asteroid = iterator.next()

            if (state.isShipAlive() && asteroid.isColliding(state.ship)) {

                asteroidsToBreak.add(asteroid)

                shipHit()
                sounds.hitShip()

            } else {

                val bullet = state.bullets.firstOrNull { asteroid.isColliding(it) }

                if (bullet != null) {
                    asteroidsToBreak.add(asteroid)
                    state.bullets.remove(bullet)
                    sounds.hitShip()
                    increaseScore(asteroid.size.points)
                }

            }

        }

        asteroidsToBreak.forEach { toBreak ->

            val newAsteroids = toBreak.split()
            newAsteroids.forEach { it.setWorldSize(viewport.worldWidth, viewport.worldHeight) }

            state.asteroids.remove(toBreak)
            state.asteroids.addAll(newAsteroids)

            if (state.networkAsteroids.contains(toBreak)) {
                state.networkAsteroids.remove(toBreak)
                state.networkAsteroids.addAll(newAsteroids)
            }

        }
    }

    private fun shipHit() {
        state.numLives--
        state.nextShipRespawnTime = state.timer + AsteroidsGameState.SHIP_RESPAWN_DELAY
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

    override fun onReceiveDamage(strength: Int) {
        val asteroids = Asteroid.spawn(strength, viewport.worldWidth, viewport.worldHeight)
        state.asteroids.addAll(asteroids)
        state.networkAsteroids.addAll(asteroids)
    }

    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer

        // Make the ship disappear when respawning. It will then reappear in the future when ready to
        // go again.
        if (state.isShipAlive()) {
            state.ship.render(camera, r)
        }

        Bullet.renderBulk(camera, r, state.bullets)
        Asteroid.renderBulk(camera, r, state.asteroids, state.networkAsteroids)

        if (lifeContainer.children.size != state.numLives) {
            redrawLives()
        }
    }

    private fun redrawLives() {
        lifeContainer.clear()
        for (i in 0 until state.numLives) {
            lifeContainer.addActor(Label("x", game.uiAssets.getStyles().label.large))
        }
    }

    override fun resizeViewport(viewportWidth: Float, viewportHeight: Float) {
        state.ship.setWorldSize(viewportWidth, viewportHeight)
        state.bullets.forEach { it.setWorldSize(viewportWidth, viewportHeight) }

        val iterator = state.asteroids.iterator()
        while (iterator.hasNext()) {
            iterator.next().setWorldSize(viewportWidth, viewportHeight)
        }

        SafeRespawnArea.screenCentre.set(viewportWidth / 2f, viewportHeight / 2f)
    }

}
