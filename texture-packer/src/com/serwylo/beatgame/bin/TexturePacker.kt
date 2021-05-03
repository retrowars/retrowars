package com.serwylo.beatgame.bin

import com.badlogic.gdx.tools.texturepacker.TexturePacker

fun main(arg: Array<String>) {
    val settings = TexturePacker.Settings()
    settings.maxWidth = 2048
    settings.maxHeight = 2048
    settings.grid = false

    TexturePacker.process(settings, "sprites/", "../android/assets", "sprites")
}