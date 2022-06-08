package com.serwylo.retrowars.games.tetris

import com.serwylo.retrowars.audio.SoundLibrary

class TetrisSoundLibrary: SoundLibrary(
    mapOf(
        "tetris_tick" to "tetris_tick.ogg",
        "tetris_clear_lines" to "tetris_clear_lines.ogg",
        "tetris_drop_piece" to "tetris_drop_piece.ogg",
        "tetris_screen_full" to "tetris_screen_full.ogg",
    )
) {

    fun tick() = play("tetris_tick")
    fun clearLines() = play("tetris_clear_lines")
    fun dropPiece() = play("tetris_drop_piece")
    fun screenFull() = play("tetris_screen_full")

}