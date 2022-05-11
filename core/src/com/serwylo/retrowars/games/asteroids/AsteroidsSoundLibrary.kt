package com.serwylo.retrowars.games.asteroids

import com.serwylo.retrowars.audio.SoundLibrary

class AsteroidsSoundLibrary: SoundLibrary() {

    fun fire() = play("asteroids_fire")
    fun hitShip() = play("asteroids_hit_ship")
    fun hitTinyAsteroid() = play("asteroids_hit_asteroid_tiny")
    fun hitSmallAsteroid() = play("asteroids_hit_asteroid_small")
    fun hitMediumAsteroid() = play("asteroids_hit_asteroid_medium")
    fun hitLargeAsteroid() = play("asteroids_hit_asteroid_large")
    fun startThrust() = startLoop("asteroids_thrust")
    fun stopThrust() = stopLoop("asteroids_thrust")

}