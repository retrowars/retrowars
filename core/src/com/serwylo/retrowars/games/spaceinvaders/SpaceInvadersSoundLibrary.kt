package com.serwylo.retrowars.games.spaceinvaders

import com.serwylo.retrowars.audio.SoundLibrary

class SpaceInvadersSoundLibrary: SoundLibrary(
    mapOf(
        "spaceinvaders_tick" to "spaceinvaders_tick.ogg",
        "spaceinvaders_alien_fire" to "spaceinvaders_alien_fire.ogg",
        "spaceinvaders_ship_fire" to "spaceinvaders_ship_fire.ogg",
        "spaceinvaders_hit_ship" to "spaceinvaders_hit_ship.ogg",
        "spaceinvaders_hit_barrier" to "spaceinvaders_hit_barrier.ogg",
        "spaceinvaders_hit_alien" to "spaceinvaders_hit_alien.ogg",
    )
) {

    fun tick() = play("spaceinvaders_tick")
    fun alienFire() = play("spaceinvaders_alien_fire")
    fun shipFire() = play("spaceinvaders_ship_fire")
    fun hitShip() = play("spaceinvaders_hit_ship")
    fun hitBarrier() = play("spaceinvaders_hit_barrier")
    fun hitAlien() = play("spaceinvaders_hit_alien")

}