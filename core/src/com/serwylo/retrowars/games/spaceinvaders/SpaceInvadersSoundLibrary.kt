package com.serwylo.retrowars.games.spaceinvaders

import com.serwylo.retrowars.audio.SoundLibrary

class SpaceInvadersSoundLibrary: SoundLibrary(
    mapOf(
    "spaceinvaders_tick" to "spaceinvaders_tick.ogg",
    )
) {

    fun tick() = play("spaceinvaders_tick")

}