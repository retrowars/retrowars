package com.serwylo.retrowars.games.snake

import com.serwylo.retrowars.audio.SoundLibrary

class SnakeSoundLibrary: SoundLibrary(
    mapOf(
    "snake_tick" to "snake_tick.ogg",
    )
) {

    fun tick() = play("snake_tick")

}