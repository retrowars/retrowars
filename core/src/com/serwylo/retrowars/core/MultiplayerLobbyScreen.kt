package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.net.RetrowarsServer

class MultiplayerLobbyScreen(private val game: RetrowarsGame): ScreenAdapter() {

    companion object {
        const val TAG = "MultiplayerLobby"
    }

    private val stage = makeStage()
    private val wrapper = Table()

    private val styles = game.uiAssets.getStyles()
    private val strings = game.uiAssets.getStrings()

    init {
        // If returning from the end of one game after hitting "play again", then we should go
        // straight to the correct screen with our existing client/server details
        val client = RetrowarsClient.get()
        val server = RetrowarsServer.get()

        // TODO: This should go into the else part of the below checks so we don't build more UI than neccesary.
        showMainLobby()

        if (server != null) {
            // TODO: But don't allow 'start' yet because all players are not yet ready... Some may be playing, some may not yet be back in the lobby yet.
            Gdx.app.log(TAG, "Returning to the lobby with an active server connection.")

            if (client == null) {
                // TODO: Something went pretty bad here, we should either kill the server and start again,
                //       or join the server again.
                Gdx.app.error(TAG, "Returned to lobby after a game, but no active client connection to go with our active server one.")
            } else {
                listenToClient(client)
                showServerLobby(client, server)
            }
        } else if (client != null) {
            Gdx.app.log(TAG, "Returning to the lobby with an active client connection.")
            listenToClient(client)
            showClientLobby(client)
        }
    }

    private fun showMainLobby() {

        val table = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)

            val heading = makeHeading(strings["multiplayer-lobby.title"], styles, strings) {
                Gdx.app.log(TAG, "Returning from lobby to main screen. Will close of anny server and/or client connection.")
                close()
                game.showMainMenu()
            }

            add(heading).center()

            row()
            add(wrapper).expand()

            wrapper.apply {

                val description = Label("Play with others\non the same local network", game.uiAssets.getStyles().label.medium)
                description.setAlignment(Align.center)

                add(description).colspan(2).spaceBottom(UI_SPACE)
                row()

                add(makeButton("Start Server", styles) {
                    startServer()
                }).right()

                add(makeButton("Join Server", styles) {
                    joinServer()
                }).left()
            }

        }

        stage.addActor(table)

    }

    private fun startServer() {

        Gdx.app.log(TAG, "Starting a new multiplayer server.")

        val server = RetrowarsServer.start()

        Gdx.app.log(TAG, "Server started. Now connecting as a client.")

        val client = createClient()

        Gdx.app.log(TAG, "Client connected.")

        showServerLobby(client, server)

    }

    private fun createClient(): RetrowarsClient {
        val client = RetrowarsClient.connect()
        listenToClient(client)
        return client
    }

    private fun listenToClient(client: RetrowarsClient) {
        client.listen(
            startGameListener = { initiateStartCountdown() },
            networkCloseListener = { wasGraceful -> game.showNetworkError(game, wasGraceful) }
        )
    }

    private fun initiateStartCountdown() {
        wrapper.apply {
            clear()

            var count = 5

            // After some experimentation, it seems the only way to get this label to animate from
            // within a table is to wrap it in a Container with isTransform = true (and don't forget
            // to enable GL_BLEND somewhere for the alpha transitions).
            val countdown = Label(count.toString(), game.uiAssets.getStyles().label.huge)
            val countdownContainer = Container(countdown).apply { isTransform = true }

            row()
            add(countdownContainer).center().expand()

            countdownContainer.addAction(
                sequence(

                    repeat(count,
                        parallel(
                            Actions.run {
                                countdown.setText((count).toString())
                                count --
                            },
                            sequence(
                                alpha(0f, 0f), // Start at 0f alpha (hence duration 0f)...
                                alpha(1f, 0.4f) // ... and animate to 1.0f quite quickly.
                            ),
                            sequence(
                                scaleTo(3f, 3f, 0f), // Start at 3x size (hence duration 0f)...
                                scaleTo(1f, 1f, 0.75f) // ... and scale back to normal size.
                            ),
                            delay(1f) // The other actions finish before the full second is up. Therefore ensure we show the counter for a full second before continuing.
                        )
                    ),

                    Actions.run {
                        Gdx.app.postRunnable {
                            val gameDetails = RetrowarsClient.get()?.me()?.getGameDetails()
                            if (gameDetails == null) {
                                // TODO: Handle this better
                                Gdx.app.log(TAG, "Unable to figure out which game to start.")
                                game.showMainMenu()
                            } else {
                                game.startGame(gameDetails.createScreen(game, gameDetails))
                            }
                        }
                    }
                )
            )

        }

    }

    private fun joinServer() {

        Gdx.app.log(TAG, "Connecting to server.")

        val client = createClient()

        Gdx.app.log(TAG, "Connected")

        showClientLobby(client)

    }

    private fun showClientLobby(client: RetrowarsClient) {
        wrapper.apply {
            clear()

            row()
            add(Label("Connected to server", styles.label.large))

            appendAvatars(this, client)
        }
    }

    private fun appendAvatars(table: Table, client: RetrowarsClient, server: RetrowarsServer? = null) {

        val infoLabel = Label("", game.uiAssets.getStyles().label.medium)
        val startButton = if (server == null) null else makeLargeButton("Start Game", game.uiAssets.getStyles()) {
            server.startGame()
        }

        table.row()
        table.add(infoLabel)

        if (startButton != null) {
            table.row()
            table.add(startButton).spaceTop(UI_SPACE)
        }

        table.row()
        val avatarCell = table.add().expandY()

        val renderPlayers = { toShow: List<Player> ->
            avatarCell.clearActor()
            avatarCell.setActor<Actor>(makeAvatarTiles(toShow, game.uiAssets))

            if (server != null) {
                when {
                    toShow.size <= 1 -> {
                        infoLabel.setText("Waiting for others to join...")
                        startButton?.isDisabled = true
                        startButton?.touchable = Touchable.disabled
                    }
                    toShow.any { it.status != Player.Status.lobby } -> {
                        infoLabel.setText("Waiting for all players to return to the lobby...")
                        startButton?.isDisabled = true
                        startButton?.touchable = Touchable.disabled
                    }
                    else -> {
                        infoLabel.setText("Ready to play!")
                        startButton?.isDisabled = false
                        startButton?.touchable = Touchable.enabled
                    }
                }
            } else {
                infoLabel.setText("Waiting for server to begin game...")
            }

            // TODO: table.invalidate() so the function just depends on its input??
            wrapper.invalidate()
        }

        // If returning to a game, we already have a list of players.
        // If it is a new game, we will have zero (not even ourselves) and will need to
        // rely on the playersChangedListener below.
        val originalPlayers: List<Player> = client.players
        if (originalPlayers.isNotEmpty()) {
            Gdx.app.log(TAG, "Showing list of existing clients.")
            renderPlayers(originalPlayers)
        }

        client.listen(
            startGameListener = { initiateStartCountdown() },
            networkCloseListener = { wasGraceful -> game.showNetworkError(game, wasGraceful) },
            playersChangedListener = { updatedPlayers -> renderPlayers(updatedPlayers) },
            playerStatusChangedListener = { _, _ -> renderPlayers(client.players) }
        )

    }

    private fun makeAvatarTiles(players: List<Player>, uiAssets: UiAssets) = Table().apply {

        pad(UI_SPACE)

        row().space(UI_SPACE).pad(UI_SPACE)
        add(Avatar(players[0], uiAssets))
        add(makeGameIcon(players[0].getGameDetails(), uiAssets))
        add(Label("You", uiAssets.getStyles().label.large))

        if (players.size > 1) {

            players.subList(1, players.size).forEach { player ->

                row().space(UI_SPACE).pad(UI_SPACE)

                add(Avatar(player, uiAssets))
                add(makeGameIcon(player.getGameDetails(), uiAssets))

                add(
                    Label(
                        when(player.status) {
                            Player.Status.playing -> "Playing"
                            Player.Status.dead -> "Dead"
                            Player.Status.lobby -> "Ready"
                            else -> "?"
                        },
                        uiAssets.getStyles().label.medium
                    )
                )

            }
        }

    }

    private fun showServerLobby(client: RetrowarsClient, server: RetrowarsServer) {
        wrapper.apply {
            clear()

            row()
            add(Label("Server started", styles.label.large))

            appendAvatars(this, client, server)
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    private fun close() {
        RetrowarsServer.stop()
        RetrowarsClient.disconnect()
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
