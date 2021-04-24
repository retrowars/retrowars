package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.asteroids.entities.Asteroid
import com.serwylo.retrowars.games.asteroids.entities.Bullet
import com.serwylo.retrowars.games.asteroids.entities.Ship
import java.util.*

class AsteroidsGameState(worldWidth: Float, worldHeight: Float) {

    var numLives: Int = 3

    val ship = Ship(Vector2(worldWidth / 2, worldHeight / 2))

    val bullets = LinkedList<Bullet>()
    val asteroids = mutableListOf<Asteroid>()
    var currentNumAsteroids = 3

    /**
     * Seconds elapsed since the game began. Wont count during pause.
     */
    var timer = 0f

    var nextRespawnTime = -1f

    init {
        ship.setWorldSize(worldWidth, worldHeight)
        asteroids.addAll(Asteroid.spawn(currentNumAsteroids, worldWidth, worldHeight))
    }


}
