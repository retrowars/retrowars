package com.serwylo.retrowars.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.Avatar
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeGameIcon
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.net.Player

private const val TAG = "networkUi"

/**
 * Players who did not join half way through a game. Could be alive or dead, but not pending.
 */
fun filterActivePlayers(players: Collection<Player>) =
    players.filter { it.status != Player.Status.pending }

fun filterAlivePlayers(players: Collection<Player>) =
    players.filter { it.status == Player.Status.playing }

/**
 * @param showDeaths If true, will display a red cross over dead players.
 */
fun createPlayerSummaries(me: Player, scores: Map<Player, Long>, showDeaths: Boolean, assets: UiAssets): Table {

    val table = Table()

    val alivePlayers = filterAlivePlayers(scores.keys)

    filterActivePlayers(scores.keys).sortedByDescending { scores[it] ?: 0 }
        .forEach { player ->

            table.pad(UI_SPACE)
            table.row().space(UI_SPACE).pad(UI_SPACE)

            table.add(
                Avatar(player, assets).apply {
                    isDead = showDeaths && player.status == Player.Status.dead
                }
            ).right()

            val gameDetails = Games.all.find { it.id == player.game }
            if (gameDetails != null) {
                table.add(makeGameIcon(gameDetails, assets))
            } else {
                Gdx.app.error(TAG, "Unsupported game for player ${player.id}: ${player.game}")
            }

            table.add(
                summarisePlayerStatus(
                    player,
                    isMe = player.id == me.id,
                    isLastPlayerStanding = alivePlayers.size == 1 && alivePlayers.contains(player),
                    score = scores[player] ?: 0,
                    highestScore = scores.values.maxOrNull() ?: 0,
                    assets
                )).left()
        }

    return table
}

/**
 * If you are the current player, show a large "You" label.
 * If the player is still alive, explain that they are still playing, otherwise put a red cross through their avatar.
 * If they are the last player alive, show how many points they need to win.
 * Always show the score for the player.
 */
private fun summarisePlayerStatus(
    player: Player,
    isMe: Boolean,
    isLastPlayerStanding: Boolean,
    score: Long,
    highestScore: Long,
    assets: UiAssets
) = VerticalGroup().apply {

    val styles = assets.getStyles()
    val strings = assets.getStrings()

    columnAlign(Align.left)

    if (isMe) {
        val youLabel = Label("You", styles.label.large)
        addActor(youLabel)
    }

    if (player.status == Player.Status.playing) {
        val stillPlayingLabel = Label(strings["end-multiplayer.still-playing"], styles.label.medium)
        addActor(stillPlayingLabel)

        val scoreNeeded = highestScore + 1 - score
        if (isLastPlayerStanding && scoreNeeded > 0) {
            val needsMoreLabel = Label("Needs $scoreNeeded more point${if (scoreNeeded == 1L) "" else "s" } to win!", styles.label.medium)
            addActor(needsMoreLabel)
        }
    }

    val scoreString = score.toString()
    val scoreLabel = Label(scoreString, styles.label.medium)
    addActor(scoreLabel)
}

fun roughTimeAgo(timestamp: Long): String {
    if (timestamp <= 0) {
        return "A long time ago"
    }

    val seconds = (System.currentTimeMillis() - timestamp) / 1000
    if (seconds < 60) {
        return "$seconds seconds ago"
    }

    val minutes = seconds / 60
    if (minutes < 60) {
        return "$minutes minutes ago"
    }

    val hours = minutes / 60
    if (hours < 24) {
        return "$hours hours ago"
    }

    val days = hours / 24
    if (days < 365) {
        return "$days days ago"
    }

    val years = days / 365
    return "$years years ago"
}