package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound

abstract class SoundLibrary {

    private val soundDefinitions = mapOf(
        "asteroids_fire" to "asteroids_fire.ogg",
        "asteroids_hit_asteroid" to "asteroids_hit_asteroid.ogg",
        "asteroids_hit_ship" to "asteroids_hit_ship.ogg",
        "asteroids_thrust" to "asteroids_thrust.ogg",
    )

    private val sounds = mutableMapOf<String, Sound>()

    private val loopingSounds = mutableSetOf<String>()

    private val theme = findSoundThemes().firstOrNull()

    protected fun play(soundName: String) {
        getOrLoadSound(soundName).play()
    }

    private fun getOrLoadSound(soundName: String): Sound {
        val soundFileName = soundDefinitions[soundName] ?: error("Could not find sound with name: $soundName")

        val file = theme?.file(soundName) ?: Gdx.files.internal("sounds/$soundFileName")
        return sounds[soundName] ?: Gdx.audio.newSound(file).also { sound ->
            Gdx.app.log("SoundLibrary", "Loading sound $soundName")
            sounds[soundName] = sound
        }
    }

    protected fun startLoop(soundName: String) {
        if (!loopingSounds.contains(soundName)) {
            getOrLoadSound(soundName).loop()
            loopingSounds.add(soundName)
        }
    }

    protected fun stopLoop(soundName: String) {
        getOrLoadSound(soundName).stop()
        loopingSounds.remove(soundName)
    }

}