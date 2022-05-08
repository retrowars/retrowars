package com.serwylo.retrowars.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.google.gson.JsonParser
import java.io.File

data class SoundTheme(val metadata: SoundThemeMetadata, val soundFileNames: List<String>) {

    // TODO: Allow sounds of different extensions from soundFileNames. Combine these with metadata.alternateSoundNames to make a map similar to that held by the sound library itself.
    fun file(soundFile: String): FileHandle? {
        val alternateSoundName = metadata.alternateSoundNames[soundFile]
        return when {
            alternateSoundName != null -> Gdx.files.external("/retrowars/sounds/${metadata.dirName}/$alternateSoundName")
            soundFileNames.contains(soundFile) -> Gdx.files.external("/retrowars/sounds/$${metadata.dirName}/$soundFile")
            else -> null
        }
    }

}

data class SoundThemeMetadata(

    @Transient
    val dirName: String,

    val title: String = "",
    val description: String = "",
    val url: String = "",
    val author: String = "",
    val alternateSoundNames: Map<String, String> = emptyMap()

)

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
            loadThemeMetadata(themeDir),
            themeDir.list().map { it.name() },
        ).apply {
            Gdx.app.log("SoundThemes", "Found theme $name with the following sounds: [${soundFileNames.joinToString(", ")}]")
        }
    }
}

private fun loadThemeMetadata(themeDir: FileHandle): SoundThemeMetadata {
    val file = File(themeDir.file(), "metadata.json")
    if (file.exists()) {
        val json = JsonParser.parseReader(file.reader(Charsets.UTF_8)).asJsonObject
        val sounds = json["alternateSoundNames"].asJsonObject
        return SoundThemeMetadata(
            themeDir.name(),
            json["title"].asString,
            json["description"].asString,
            json["url"].asString,
            json["author"].asString,
            sounds.keySet().associateWith { sounds[it].asString },
        )
    }

    return SoundThemeMetadata(themeDir.name())
}