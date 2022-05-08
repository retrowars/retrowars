package com.serwylo.retrowars.games.asteroids

import com.serwylo.retrowars.audio.SoundLibrary

class AsteroidsSoundLibrary: SoundLibrary() {

    fun fire() = play("asteroids_fire")
    fun hitShip() = play("asteroids_hit_ship")
    fun hitAsteroid() = play("asteroids_hit_asteroid")
    fun startThrust() = startLoop("asteroids_thrust")
    fun stopThrust() = stopLoop("asteroids_thrust")

}