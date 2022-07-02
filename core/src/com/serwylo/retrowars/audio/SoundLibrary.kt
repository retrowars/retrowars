package com.serwylo.retrowars.audio

import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.serwylo.retrowars.utils.Options
import kotlinx.coroutines.*

abstract class SoundLibrary(private val soundDefinitions: Map<String, String>) {

    private val availableSounds = mutableMapOf<String, List<FileHandle>>()

    private val loopingSounds = mutableMapOf<String, Sound>()
    private val loopingSoundIds = mutableMapOf<String, Long>()
    private val loopingFadingJobs = mutableMapOf<String, Job>()

    private val theme = findSoundThemes().firstOrNull()

    private val soundJob = Job()
    private val soundScope = CoroutineScope(Dispatchers.IO + soundJob)

    protected fun play(soundName: String) {
        soundScope.launch {
            getSound(soundName).play(Options.getRealSoundVolume())
        }
    }

    private fun getSound(soundName: String): Sound {
        val cachedFiles = availableSounds[soundName] ?: error("Could not find sound(s) with name: $soundName")
        val file = cachedFiles.random()
        return assetManager.get(InternalExternalFileHandleResolver.toFileName(file), Sound::class.java)
    }

    protected fun startLoop(soundName: String) {
        val looping = loopingSounds[soundName]
        if (looping == null) {

            loopingFadingJobs[soundName]?.also { job ->
                loopingFadingJobs.remove(soundName)
                job.cancel()
            }

            soundScope.launch {
                getSound(soundName).also { sound ->
                    loopingSounds[soundName] = sound

                    val id = sound.loop(Options.getRealSoundVolume())
                    loopingSoundIds[soundName] = id
                }
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

    private val assetManager = AssetManager(InternalExternalFileHandleResolver()).apply {
        soundDefinitions.entries.onEach { (soundName, defaultSoundFileName) ->
            val files = getSoundFileHandles(soundName, theme, defaultSoundFileName)
            availableSounds[soundName] = files
            files.onEach { soundFile ->
                Gdx.app.log("Loading Sounds", "Queuing sound for loading: $soundFile")
                load(InternalExternalFileHandleResolver.toFileName(soundFile), Sound::class.java)
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

/**
 * Helper class to differentiate between internal and external file handles when loading via
 * the [AssetManager].
 *
 * This exists because [AssetManager] only knows the [String] path to a file, not a proper [FileHandle].
 * Furthermore, depending on the platform the game runs on, we don't even know what class an
 * internal or external file will resolve to.
 *
 * Therefore, by first calling [InternalExternalFileHandleResolver.toFileName] this will prefix
 * the path with "internal:" or "external:". This in turn will be used when loading the asset,
 * by inspecting the path and looking for the appropriate prefix, and using Gdx.files.internal/external
 * as appropriate.
 */
class InternalExternalFileHandleResolver: FileHandleResolver {

    companion object {
        fun toFileName(handle: FileHandle): String = "${handle.type().name}:${handle.path()}"
    }

    override fun resolve(fileName: String): FileHandle {
        val colon = fileName.indexOf(":")
        val prefix = fileName.substring(0, colon)
        val suffix = fileName.substring(colon + 1)
        return when (prefix) {
            Files.FileType.Internal.name -> Gdx.files.internal(suffix)
            Files.FileType.Absolute.name -> Gdx.files.absolute(suffix)
            Files.FileType.Classpath.name -> Gdx.files.classpath(suffix)
            Files.FileType.External.name -> Gdx.files.external(suffix)
            Files.FileType.Local.name -> Gdx.files.local(suffix)
            else -> error("Unsupported fileName: $fileName. Be sure to call InternalExternalFilehandleResolver.toFileName(FileHandle) to prefix the file path before requesting an asset.")
        }
    }

}