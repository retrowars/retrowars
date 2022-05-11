package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle

abstract class SoundLibrary(private val soundDefinitions: Map<String, String>) {

    private val foundSoundFiles = mutableMapOf<String, List<FileHandle>>()
    private val loadedSounds = mutableMapOf<String, Sound>()

    private val loopingSounds = mutableMapOf<String, Sound>()

    private val theme = findSoundThemes().firstOrNull()

    protected fun play(soundName: String) {
        getOrLoadSound(soundName).play()
    }

    private fun getOrLoadSound(soundName: String): Sound {
        val soundFileName = soundDefinitions[soundName] ?: error("Could not find sound with name: $soundName")

        val cachedFiles = foundSoundFiles[soundName]
        val file = if (cachedFiles != null) {
            cachedFiles.random()
        } else {
            val files = getSoundFileHandles(soundName, theme, soundFileName)
            foundSoundFiles[soundName] = files
            files.random()
        }

        val cachedSound = loadedSounds[file.path()]
        if (cachedSound != null) {
            return cachedSound
        }

        Gdx.app.log("SoundLibrary", "Loading sound $soundName from $file")
        val sound = Gdx.audio.newSound(file)
        loadedSounds[file.path()] = sound
        return sound
    }

    protected fun startLoop(soundName: String) {
        val looping = loopingSounds[soundName]
        if (looping == null) {
            getOrLoadSound(soundName).also { sound ->
                loopingSounds[soundName] = sound
                sound.loop()
            }
        }
    }

    protected fun stopLoop(soundName: String) {
        val looping = loopingSounds[soundName]
        if (looping != null) {
            looping.stop()
            loopingSounds.remove(soundName)
        }
    }

}