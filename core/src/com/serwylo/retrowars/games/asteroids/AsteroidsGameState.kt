package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.asteroids.entities.Asteroid
import com.serwylo.retrowars.games.asteroids.entities.Bullet
import com.serwylo.retrowars.games.asteroids.entities.Ship
import java.util.*

class AsteroidsGameState(worldWidth: Float, worldHeight: Float) {

    companion object {
        /**
         * After getting hit, wait this long before respawning the ship in the middle of the screen.
         * In the future, this will be the *minimum* respawn delay, as we may need to wait until
         * there are no asteroids in the way.
         */
        const val SHIP_RESPAWN_DELAY = 2f

        /**
         * After clearing the screen of asteroids, wait this long before respawning a new set of
         * asteroids around the edge of the screen.
         */
        const val ASTEROID_RESPAWN_DELAY = 3f
    }

    var numLives: Int = 3

    val ship = Ship(Vector2(worldWidth / 2, worldHeight / 2))

    val bullets = LinkedList<Bullet>()
    val asteroids = mutableListOf<Asteroid>()
    val networkAsteroids = mutableSetOf<Asteroid>()
    var currentNumAsteroids = 3

    /**
     * Seconds elapsed since the game began. Wont count during pause.
     */
    var timer = 0f

    var nextAsteroidRespawnTime = -1f
    var nextShipRespawnTime = -1f

    init {
        ship.setWorldSize(worldWidth, worldHeight)
        asteroids.addAll(Asteroid.spawn(currentNumAsteroids, worldWidth, worldHeight))
    }

    fun isShipAlive() = nextShipRespawnTime < 0
    fun isShipReadyToRespawn() = nextShipRespawnTime > 0 && nextShipRespawnTime <= timer
    fun areAsteroidsReadyToRespawn() = nextAsteroidRespawnTime > 0 && nextAsteroidRespawnTime < timer

}
