package com.serwylo.retrowars.scoring

import com.badlogic.gdx.Gdx
import com.google.gson.Gson
import com.serwylo.retrowars.games.GameDetails
import kotlin.math.max

data class HighScore(val score: Long, val timestamp: Long, val attempts: Int) {

    fun exists() = score > 0

}

fun clearAllHighScores() {
    prefs().clear()
}

fun loadHighScore(game: GameDetails): HighScore {
    val json = prefs().getString(game.id, "")
    return if (json == "") {
        HighScore(0, 0, 0)
    } else {
        Gson().fromJson(json, HighScore::class.java)
    }
}

fun saveHighScore(game: GameDetails, score: Long, force: Boolean = false): HighScore {
    val highest = loadHighScore(game)

    val toSave = if (force) {
        HighScore(score, System.currentTimeMillis(), 0)
    } else {
        HighScore(
                max(highest.score, score),
                System.currentTimeMillis(),
                highest.attempts + 1
        )
    }

    val json = Gson().toJson(toSave)

    prefs().putString(game.id, json).flush()

    return toSave
}

private fun prefs() = Gdx.app.getPreferences("com.serwylo.retrowars.scores")