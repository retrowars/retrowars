package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.net.RetrowarsServer

class EndMultiplayerGameScreen(private val game: RetrowarsGame): ScreenAdapter() {

    companion object {
        const val TAG = "EndMultiplayerGameScreen"
    }

    private val stage = makeStage()
    private val subheading: Label
    private val playerSummaries: Cell<Actor>
    private val actionButtons: Cell<Actor>

    private val uiAssets = game.uiAssets
    private val styles = game.uiAssets.getStyles()
    private val strings = game.uiAssets.getStrings()

    private val client = RetrowarsClient.get()!! // TODO: Verify this and bail with message to user if assumption is incorrect.

    init {
        val table = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)

            val heading = makeHeading(strings["end-multiplayer.title"], styles, strings)
            add(heading)

            row()
            // Will be updated immediately using "refreshScreen()", and subsequently when we get fresh info from the server.
            subheading = Label("", styles.label.large)
            add(subheading)

            row()
            playerSummaries = add()

            // When the game is finished, populate this with some sensible defaults.
            // Prior to that, we'll allow people to leave the game if they get sick of watching
            // others get really good scores after we died early.
            row()
            actionButtons = add()

            // TODO: During the game, listen for these events and then show the data in the HUD in realtime.
            client.scoreChangedListener = { _, _ -> showPlayerSummaries() }
            client.playerDiedListener = { _ -> refreshScreen() }

            refreshScreen()
        }

        stage.addActor(table)

    }

    private fun refreshScreen() {
        if (client.players.any { it.status == Player.Status.playing }) {
            showPlayerSummaries()
        } else {
            showFinalResults()
        }
    }

    private fun showFinalResults() {

        subheading.setText("Congratulations!")

        showPlayerSummaries()

        val group = HorizontalGroup().apply {

            space(UI_SPACE)

            addActor(
                makeButton("Play again", styles) {
                    game.showMultiplayerLobby()
                }
            )

            addActor(
                makeButton(if (RetrowarsServer.get() == null) "Leave game" else "End game for all players", styles) {
                    RetrowarsClient.disconnect()
                    RetrowarsServer.stop()
                    game.showMainMenu()
                }
            )

        }

        actionButtons.clearActor()
        actionButtons.setActor(group)
    }

    private fun showPlayerSummaries() {

        val table = Table()

        client.players
            .sortedByDescending { client.getScoreFor(it) }
            .forEach { player ->

               table.pad(UI_SPACE)

               table.row().space(UI_SPACE).pad(UI_SPACE)

               table.add(AvatarTile(player, uiAssets, player == client.me())).right()

               val group = VerticalGroup()
               group.align(Align.left)
               table.add(group).left()

               if (player.status == Player.Status.playing) {
                   group.addActor(Label(strings["end-multiplayer.still-playing"], styles.label.medium))
               }

               group.addActor(Label(client.getScoreFor(player).toString(), styles.label.medium))

           }

        playerSummaries.clearActor()
        playerSummaries.setActor(table)

    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun render(delta: Float) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

}