package com.serwylo.retrowars.games.snake

import com.serwylo.retrowars.audio.SoundLibrary

class SnakeSoundLibrary: SoundLibrary(
    mapOf(
        "snake_tick" to "snake_tick.ogg",
        "snake_eat" to "snake_eat.ogg",
        "snake_hit" to "snake_hit.ogg",
    )
) {

    fun tick() = play("snake_tick")
    fun eat() = play("snake_eat")
    fun hit() = play("snake_hit")

}