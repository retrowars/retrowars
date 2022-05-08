package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

data class SoundTheme(val name: String, val soundFileNames: List<String>) {
    fun file(soundFile: String): FileHandle? = if (soundFileNames.contains(soundFile)) {
        Gdx.files.external("/retrowars/sounds/$name/$soundFile")
    } else {
        null
    }
}

fun findSoundThemes(): List<SoundTheme> {
    val dir = Gdx.files.external("/retrowars/sounds/")

    Gdx.app.log("SoundThemes", "Looking for sound themes in ${dir.file().absolutePath}.")

    if (!dir.exists()) {
        Gdx.app.log("SoundThemes", "Directory does not exist, will use default sounds.")
        return emptyList()
    }

    return dir.list().map { themeDir ->
        val name = themeDir.name()
        SoundTheme(
            name,
            themeDir.list().map { it.name() },
        ).apply {
            Gdx.app.log("SoundThemes", "Found theme $name with the following sounds: [${soundFileNames.joinToString(", ")}]")
        }
    }
}