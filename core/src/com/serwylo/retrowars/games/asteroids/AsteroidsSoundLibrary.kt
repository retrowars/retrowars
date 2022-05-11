package com.serwylo.retrowars.games.asteroids

import com.serwylo.retrowars.audio.SoundLibrary

class AsteroidsSoundLibrary: SoundLibrary(
    mapOf(
    "asteroids_fire" to "asteroids_fire.ogg",
    "asteroids_hit_asteroid_tiny" to "asteroids_hit_asteroid_tiny.ogg",
    "asteroids_hit_asteroid_small" to "asteroids_hit_asteroid_small.ogg",
    "asteroids_hit_asteroid_medium" to "asteroids_hit_asteroid_medium.ogg",
    "asteroids_hit_asteroid_large" to "asteroids_hit_asteroid_large.ogg",
    "asteroids_hit_ship" to "asteroids_hit_ship.ogg",
    "asteroids_thrust" to "asteroids_thrust.ogg",
    )
) {

    fun fire() = play("asteroids_fire")
    fun hitShip() = play("asteroids_hit_ship")
    fun hitTinyAsteroid() = play("asteroids_hit_asteroid_tiny")
    fun hitSmallAsteroid() = play("asteroids_hit_asteroid_small")
    fun hitMediumAsteroid() = play("asteroids_hit_asteroid_medium")
    fun hitLargeAsteroid() = play("asteroids_hit_asteroid_large")
    fun startThrust() = startLoop("asteroids_thrust")
    fun stopThrust() = stopLoop("asteroids_thrust")

}