package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class SoundTheme(val metadata: SoundThemeMetadata, val soundFiles: List<FileHandle>) {
    override fun toString() = "SoundTheme: ${metadata.dirName}, soundFiles: [${soundFiles.joinToString(", ") { it.name() }}]"
}

fun getSoundFileHandlesFromTheme(soundName: String, theme: SoundTheme): List<FileHandle> {
    val alternateSound = theme.metadata.alternateSoundNames[soundName]
    if (alternateSound != null) {
        Gdx.app.debug("SoundThemes", "Loading sound $soundName from sound theme ${theme.metadata.dirName} from the alternate named ${alternateSound.name()}.")
        return listOf(alternateSound)
    }

    val sounds = theme.soundFiles.filter {
        Regex("$soundName\\.\\d+\\.(${supportedSoundExtensions.joinToString("|")})").matches(it.name())
    }

    if (sounds.isNotEmpty()) {
        Gdx.app.debug("SoundThemes", "Loading ${sounds.size} versions of $soundName sounds from theme ${theme.metadata.dirName}: [${sounds.joinToString(", ") { it.name() }}]")
        return sounds
    }

    val sound = theme.soundFiles.firstOrNull {
        it.nameWithoutExtension() == soundName
    }

    if (sound != null) {
        Gdx.app.debug("SoundThemes", "Loading sound $soundName from sound theme ${theme.metadata.dirName} from ${sound.name()}.")
        return listOf(sound)
    }

    return emptyList()
}

private fun appendNumericSuffix(fileName: String, n: Int): String {
    val file = File(fileName)
    val name = file.nameWithoutExtension
    val ext = file.extension

    return "$name.$n.$ext"
}

private fun internalIndexedFile(filename: String, n: Int): FileHandle? {
    val name = appendNumericSuffix(filename, n)
    val file = Gdx.files.internal("sounds/$name")

    return if (file.exists()) file else null
}

fun getDefaultSoundFileHandles(defaultSoundFileName: String): List<FileHandle> {
    var index = 1
    val firstFile = internalIndexedFile(defaultSoundFileName, index ++)
        ?: return listOf(Gdx.files.internal("sounds/$defaultSoundFileName"))

    val files = mutableListOf(firstFile)
    while (true) {
        val file = internalIndexedFile(defaultSoundFileName, index ++) ?: break
        files.add(file)
    }

    return files.toList()
}

fun getSoundFileHandles(soundName: String, theme: SoundTheme?, defaultSoundFileName: String): List<FileHandle> {
    if (theme != null) {
        val sounds = getSoundFileHandlesFromTheme(soundName, theme)
        if (sounds.isNotEmpty()) {
            return sounds
        }

        Gdx.app.debug("SoundThemes", "Sound $soundName not found in theme ${theme.metadata.dirName}, will use game default.")
    } else {
        Gdx.app.debug("SoundThemes", "Sound theme doesn't exist, so fetching $soundName from the game default: $defaultSoundFileName.")
    }

    return getDefaultSoundFileHandles(defaultSoundFileName)
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

    val soundsData = metadataJson["alternateSoundNames"]?.asJsonObject ?: return emptyMap()

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