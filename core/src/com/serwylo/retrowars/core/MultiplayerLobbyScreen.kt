package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
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
                showServerLobby()
            }
        } else if (client != null) {
            Gdx.app.log(TAG, "Returning to the lobby with an active client connection.")
            listenToClient(client)
            showClientLobby()
        }
    }

    private fun showMainLobby() {

        val table = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)

            row()

            val heading = makeHeading(strings["multiplayer-lobby.title"], styles, strings) {
                close()
                game.showMainMenu()
            }

            add(heading).center()

            row()
            add(wrapper).expand()

            wrapper.add(
                makeButton("Start Server", styles) {
                    startServer()
                }
            )

            wrapper.add(
                makeButton("Join Server", styles) {
                    joinServer()
                }
            )

        }

        stage.addActor(table)

    }

    private fun startServer() {

        Gdx.app.log(TAG, "Starting a new multiplayer server.")

        RetrowarsServer.start()
        createClient()

        Gdx.app.log(TAG, "Server started.")

        showServerLobby()

    }

    private fun createClient() {
        val client = RetrowarsClient.connect()
        listenToClient(client)
    }

    private fun listenToClient(client: RetrowarsClient) {
        client.startGameListener = {
            initiateStartCountdown()

            // If we play multiple games in succession, don't want to have old listeners
            // floating around.
            client.startGameListener = null
        }
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

        createClient()

        Gdx.app.log(TAG, "Connected")

        showClientLobby()

    }

    private fun showClientLobby() {
        wrapper.apply {
            clear()

            row()
            add(Label("Connected to server. Waiting for others to join...", styles.label.large))

            appendAvatars(this)
        }
    }

    private fun appendAvatars(table: Table) {

        table.row()
        val avatarCell = table.add().expandY()

        // If returning to a game, we already have a list of players.
        // If it is a new game, we will have zero (not even outselves) and will need to
        // rely on the playersChangedListener below.
        val players: List<Player> = RetrowarsClient.get()?.players ?: emptyList()
        if (players.isNotEmpty()) {
            avatarCell.setActor<HorizontalGroup>(makeAvatarTiles(players, game.uiAssets))
        }

        RetrowarsClient.get()?.playersChangedListener = { players ->
            Gdx.app.log(TAG, "Updating list of clients to show.")

            avatarCell
                .clearActor()
                .setActor<HorizontalGroup>(makeAvatarTiles(players, game.uiAssets))

            wrapper.invalidate()
        }

    }

    private fun showServerLobby() {
        wrapper.apply {
            clear()

            row()
            add(Label("Server started. Waiting for others to join...", styles.label.large))

            appendAvatars(this)

            row()
            add(makeLargeButton("Start", styles) {
                RetrowarsServer.get()?.startGame()
            })
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
