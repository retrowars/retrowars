package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class SoundTheme(val metadata: SoundThemeMetadata, private val soundFiles: List<FileHandle>) {

    private val soundNamesToFiles = soundFiles.associateBy { it.nameWithoutExtension() }

    fun file(soundFile: String): FileHandle? {
        val alternateSound = metadata.alternateSoundNames[soundFile]
        if (alternateSound != null) {
            Gdx.app.log("SoundThemes", "Loading sound $soundFile from sound theme ${metadata.dirName} from the alternate named ${alternateSound.name()}.")
            return alternateSound
        }

        val sound = soundNamesToFiles[soundFile]
        if (sound != null) {
            Gdx.app.log("SoundThemes", "Loading sound $soundFile from sound theme ${metadata.dirName} from ${sound.name()}.")
            return sound
        }

        Gdx.app.log("SoundThemes", "Sound $soundFile not found in theme ${metadata.dirName}, will use game default.")
        return null
    }

    override fun toString() = "SoundTheme: ${metadata.dirName}, soundFiles: [${soundFiles.map { it.name() }.joinToString(", ")}]"

}

/**
 * Most of this is populated from the `metadata.json` file at the top level of the sound theme's directory.
 * The exception is `dirName` which is the name of that directory.
 */
data class SoundThemeMetadata(

    @Transient
    val dirName: String,

    val title: String = "",
    val description: String = "",
    val url: String = "",
    val author: String = "",

    /**
     * If you want to use different filenames then those specified by retrowars, you can do so by providing
     * an object that maps the original name in the game without extension (e.g. "asteroids_fire") and mapping
     * it to a new filename, again without extension, (e.g. "shoot").
     *
     * This allows renaming for renaming sake, but also using one sound for multiple different things:
     *
     *   "alternateSoundNames": {
     *     "asteroids_fire": "boing",
     *     "asteroids_hit_ship: "boing"
     *   }
     */
    val alternateSoundNames: Map<String, FileHandle> = emptyMap()

)

fun findSoundThemes(): List<SoundTheme> {
    val dir = Gdx.files.external("/retrowars/sounds/")

    Gdx.app.log("SoundThemes", "Looking for sound themes in ${dir.file().absolutePath} (supported extensions: ${supportedSoundExtensions.joinToString(", ")}).")

    if (!dir.exists()) {
        Gdx.app.log("SoundThemes", "Directory does not exist, will use default sounds.")
        return emptyList()
    }

    return dir.list().map { themeDir ->
        SoundTheme(
            loadThemeMetadata(themeDir),
            findSounds(themeDir),
        ).apply {
            Gdx.app.log("SoundThemes", "Found theme: $this")
        }
    }
}

private val supportedSoundExtensions = setOf("ogg", "mp3", "wav")

private fun findSounds(themeDir: FileHandle): List<FileHandle> =
    themeDir
        .list()
        .filter { supportedSoundExtensions.contains( it.extension() ) }

// TODO: Make this more robust so that it skips broken things but still provides as much as possible in case of error
//       (e.g. single file missing from alternateSoundNames should not crash the whole thing, just skip that one).
private fun loadThemeMetadata(themeDir: FileHandle): SoundThemeMetadata {
    val file = File(themeDir.file(), "metadata.json")
    if (file.exists()) {
        val json = JsonParser.parseReader(file.reader(Charsets.UTF_8)).asJsonObject
        return SoundThemeMetadata(
            themeDir.name(),
            json["title"]?.asString ?: "",
            json["description"]?.asString ?: "",
            json["url"]?.asString ?: "",
            json["author"]?.asString ?: "",
            loadAlternativeSounds(themeDir, json),
        )
    }

    return SoundThemeMetadata(themeDir.name())

}

private fun loadAlternativeSounds(themeDir: FileHandle, metadataJson: JsonObject): Map<String, FileHandle> {

    val soundsData = metadataJson["alternateSoundNames"]?.asJsonObject

    if (soundsData == null) {
        return emptyMap()
    }

    return soundsData
        .keySet()
        .associateWith { soundName ->
            val soundFile = soundsData[soundName].asString
            val safeSoundFile = File(soundFile).name // Don't allow directory traversal (e.g. ../../../secret-recording.mp3)
            Gdx.files.external("/retrowars/sounds/${themeDir.name()}/${safeSoundFile}")
        }
        .filter { (soundName, file) ->
            if (!file.exists()) {
                Gdx.app.log("SoundThemes", "Ignoring ${themeDir.name()}.alternativeSoundNames.${soundName} because ${file.name()} doesn't exist.")
                false
            } else {
                true
            }
        }
}