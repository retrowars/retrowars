package com.serwylo.retrowars.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
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

fun isLastPlayerStanding(player: Player?, allPlayers: Collection<Player>): Boolean {
    val alive = filterAlivePlayers(allPlayers)
    return alive.size == 1 && alive[0].id == player?.id
}

val ENEMY_ATTACK_COLOUR: Color = Color.RED

/**
 * @param showDeaths If true, will display a red cross over dead players.
 * @param playersToShow Defaults to active players.
 */
fun createPlayerSummaries(assets: UiAssets, me: Player, scores: Map<Player, Long>, showDeaths: Boolean, playersToShow: List<Player>? = null): Table {

    val table = Table()

    val players = playersToShow ?: filterActivePlayers(scores.keys)
    val alivePlayers = filterAlivePlayers(players)

    players.sortedByDescending { scores[it] ?: 0 }
        .forEach { player ->

            table.row().pad(UI_SPACE / 2)

            val isDead = showDeaths && player.status == Player.Status.dead

            val gameDetails = Games.all.find { it.id == player.game }
            if (gameDetails != null) {
                table.add(
                    makeAvatarAndGameIcon(player.id, isDead, gameDetails, assets)
                )
            } else {
                Gdx.app.error(TAG, "Unsupported game for player ${player.id}: ${player.game}")
                table.add(
                    Avatar(player.id, assets, isDead)
                )
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

fun makeContributeServerInfo(styles: UiAssets.Styles) = VerticalGroup().apply {
    space(UI_SPACE)
    addActor(Label("Want to help the Super Retro Mega Wars project\nby running a public server?", styles.label.small).apply {
        setAlignment(Align.center)
    })
    addActor(
        makeSmallButton("Learn how to help", styles) {
            Gdx.net.openURI("https://github.com/retrowars/retrowars-servers/#contributing")
        }
    )
}

fun roughTimeAgo(timestamp: Long): String {
    if (timestamp <= 0) {
        return "A long time ago"
    }

    val seconds = (System.currentTimeMillis() - timestamp) / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Seconds ago"
        minutes < 60 -> "Minutes ago"
        hours < 24 -> "Hours ago"
        days < 30 -> "Days ago"
        days < 365 -> "Months ago"
        else -> "Years ago"
    }
}
