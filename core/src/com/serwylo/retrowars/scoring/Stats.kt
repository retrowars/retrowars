package com.serwylo.retrowars.scoring

import com.badlogic.gdx.Gdx
import com.google.gson.Gson
import com.serwylo.retrowars.games.Games

/**
 * Stats are collected and stored offline (never sent over the network - we value your privacy!).
 * Why do we collect them? For balancing purposes, it is helpful if people are able to share their
 * experiences playing the game. By recording how long each game takes, and how many points were
 * scored, we can compare games to each other to better balance multi-player games. The end goal
 * should be that no matter what the game is, players score the same number of points per minute
 * on average.
 */

data class Stats(val duration: Long, val score: Long, val gameType: String) {

    private val seconds = duration / 1000f
    private val minutes = seconds / 60f

    fun scorePerSecond() = if (seconds == 0f) 0 else score / seconds
    fun scorePerMin() = if (minutes == 0f) 0 else score / minutes

}

fun recordStats(stats: Stats) {
    val json = Gson().toJson(stats)
    prefs().putString("${System.currentTimeMillis()}-${stats.gameType}", json).flush()
    dumpStats()
}

fun dumpStats() {

    val tag = "stats"

    Gdx.app.log(tag, "gameId,plays,avgScore,avgDuration,scorePerSecond,scorePerMinute")

    val stats = loadAllStats()

    Games.allAvailable.forEach { game ->

        val gameStats = stats.filter { it.gameType == game.id }

        if (gameStats.isEmpty()) {

            Gdx.app.log(tag, "${game.id},0,0,0,0,0")

        } else {

            val total = gameStats.fold(Stats(0, 0, game.id)) { acc, stats ->
                Stats(acc.duration + stats.duration, acc.score + stats.score, game.id)
            }

            val avg = Stats(total.duration / gameStats.size, total.score / gameStats.size, game.id)

            Gdx.app.log(tag, "${game.id},${gameStats.size},${avg.score},${avg.duration},${avg.scorePerSecond()},${avg.scorePerMin()}")

        }

    }
}

fun loadAllStats(): List<Stats> {

    val values = mutableListOf<String>()

    prefs().get().values.forEach {
        if (it is String) {
            values.add(it)
        }
    }

    return values.map {
        Gson().fromJson(it, Stats::class.java)
    }

}

private fun prefs() = Gdx.app.getPreferences("com.serwylo.retrowars.stats")