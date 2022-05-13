package com.serwylo.retrowars.games.tetris

import com.serwylo.retrowars.audio.SoundLibrary

class TetrisSoundLibrary: SoundLibrary(
    mapOf(
    "tetris_tick" to "tetris_tick.ogg",
    )
) {

    fun tick() = play("tetris_tick")

}