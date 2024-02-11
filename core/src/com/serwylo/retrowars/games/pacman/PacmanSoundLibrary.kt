package com.serwylo.retrowars.games.pacman

import com.serwylo.retrowars.audio.SoundLibrary

class PacmanSoundLibrary: SoundLibrary(
    mapOf(
        "pacman_tick" to "snake_tick.ogg",
        "pacman_eat" to "snake_eat.ogg",
    )
) {

    fun tick() = play("pacman_tick")
    fun eat() = play("pacman_eat")

}