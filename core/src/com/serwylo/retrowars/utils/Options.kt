package com.serwylo.retrowars.utils

import com.badlogic.gdx.Gdx
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.Games
import kotlin.random.Random

object Options {

    private var useVisualEffects: Boolean = prefs().getBoolean("useVisualEffects", true)

    private var playerId: Long = prefs().getLong("playerId", Random.nextLong())

    private var mute: Boolean = prefs().getBoolean("mute", false)

    private var softControllers: MutableMap<GameDetails, Int> = Games.all.associateWith {
        prefs().getInteger("${it.id}-softController", 0)
    }.toMutableMap()

    fun useVisualEffects(): Boolean {
        return useVisualEffects
    }

    fun setUseVisualEffects(use: Boolean) {
        prefs().putBoolean("useVisualEffects", use).flush()
        useVisualEffects = use
    }

    fun getPlayerId(): Long {
        return playerId
    }

    fun setPlayerId(value: Long) {
        prefs().putLong("playerId", value).flush()
        playerId = value
    }

    fun isMute(): Boolean {
        return mute
    }

    fun setMute(mute: Boolean) {
        prefs().putBoolean("mute", mute).flush()
        this.mute = mute
    }

    fun getSoftController(game: GameDetails): Int {
        return softControllers[game]!!
    }

    fun setSoftController(game: GameDetails, index: Int) {
        prefs().putInteger("${game.id}-softController", index).flush()
        softControllers[game] = index
    }

    private fun prefs() = Gdx.app.getPreferences("com.serwylo.retrowars.options")

    fun forceRandomPlayerId() {
        playerId = Random.nextLong()
    }

}