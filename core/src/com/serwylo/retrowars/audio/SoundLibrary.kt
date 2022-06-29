package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.serwylo.retrowars.utils.Options
import io.ktor.server.engine.*
import kotlinx.coroutines.*

abstract class SoundLibrary(private val soundDefinitions: Map<String, String>) {

    private val foundSoundFiles = mutableMapOf<String, List<FileHandle>>()

    private val loopingSounds = mutableMapOf<String, Sound>()
    private val loopingSoundIds = mutableMapOf<String, Long>()
    private val loopingFadingJobs = mutableMapOf<String, Job>()

    private val theme = findSoundThemes().firstOrNull()

    private val soundJob = Job()
    private val soundScope = CoroutineScope(Dispatchers.IO + soundJob)

    protected fun play(soundName: String) {
        getOrLoadSound(soundName).play(Options.getRealSoundVolume())
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

        return assetManager.get(file.path(), Sound::class.java)
    }

    protected fun startLoop(soundName: String) {
        val looping = loopingSounds[soundName]
        if (looping == null) {

            loopingFadingJobs[soundName]?.also { job ->
                loopingFadingJobs.remove(soundName)
                job.cancel()
            }

            getOrLoadSound(soundName).also { sound ->
                loopingSounds[soundName] = sound

                val id = sound.loop(Options.getRealSoundVolume())
                loopingSoundIds[soundName] = id
            }
        }
    }

    protected fun stopLoop(soundName: String, stopType: StopType = StopType.Immediately) {
        val looping = loopingSounds[soundName]
        val previousId = loopingSoundIds[soundName]

        loopingSounds.remove(soundName)
        loopingSoundIds.remove(soundName)
        loopingFadingJobs.remove(soundName)

        if (looping == null) {
            return
        }

        // This shouldn't happen, but be defensive just in case and don't
        // bother trying to fade out if this is the case.
        if (previousId == null) {
            looping.stop()
            return
        }

        if (Options.getRealSoundVolume() <= 0f) {
            looping.stop(previousId)
            return
        }

        when (stopType) {
            StopType.Immediately -> looping.stop(previousId)
            StopType.FadeFast -> {

                soundScope.launch {
                    var volume = 1f
                    while (volume > 0f) {
                        delay(50)
                        volume -= 0.2f
                        looping.setVolume(previousId, volume)
                    }
                    yield()
                    looping.stop(previousId)

                }.also { job ->
                    loopingFadingJobs[soundName] = job

                    // If somebody has cancelled this because, e.g. they are starting
                    // their own version of this sound, then we should make sure to do
                    // a hard stop so that they can start playing properly.
                    // If we finished normally, then we should also clean up our job
                    // by removing it from the list.
                    job.invokeOnCompletion {
                        loopingFadingJobs.remove(soundName)
                        looping.stop(previousId)
                    }
                }

            }
        }
    }

    private val assetManager = AssetManager(InternalFileHandleResolver()).apply {
        soundDefinitions.entries.onEach { (soundName, defaultSoundFileName) ->
            getSoundFileHandles(soundName, theme, defaultSoundFileName).onEach { soundFile ->
                Gdx.app.log("Loading Sounds", "Queuing sound for loading: ${soundFile}")
                load(soundFile.path(), Sound::class.java)
            }
        }
    }

    fun isLoaded(): Boolean {
        return assetManager.update()
    }

    fun dispose() {
        assetManager.dispose()
    }

    enum class StopType {
        Immediately,
        FadeFast,
    }

}