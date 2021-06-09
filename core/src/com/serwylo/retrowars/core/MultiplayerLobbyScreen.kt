package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.net.RetrowarsServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MultiplayerLobbyScreen(game: RetrowarsGame): Scene2dScreen(game, {
    close()
    game.showMainMenu()
}) {

    companion object {
        const val TAG = "MultiplayerLobby"
        const val STATE_TAG = "MultiplayerLobby - State"

        private fun close() {
            // TODO: Move to coroutine and show status to user...
            RetrowarsClient.get()?.listen(
                // Don't do anything upon network close, because we know we are about to shut down our
                // own server.
                networkCloseListener = {}
            )

            RetrowarsServer.stop()
            RetrowarsClient.disconnect()
        }
    }

    private val wrapper = Table()

    private val styles = game.uiAssets.getStyles()
    private val strings = game.uiAssets.getStrings()

    /**
     * The next state we need to transition to will generally be triggered on a network thread, so
     * lets queue them up, ready to be actioned on the next main thread frame.
     */
    private var stateLock = Object()
    private var currentState: UiState = Splash()
    private var renderedState: UiState? = null

    init {
        stage.addActor(makeStageDecoration())
    }

    private fun makeStageDecoration(): Table {
        return Table().apply {
            setFillParent(true)
            pad(UI_SPACE)

            val heading = makeHeading(strings["multiplayer-lobby.title"], styles, strings) {
                GlobalScope.launch {
                    Gdx.app.log(TAG, "Returning from lobby to main screen. Will close of anny server and/or client connection.")
                    close()
                    game.showMainMenu()
                }
            }

            add(heading).center()

            row()
            add(wrapper).expand()
        }
    }

    private fun changeState(action: Action) {
        synchronized(stateLock) {
            val oldState = currentState
            val newState = currentState.consumeAction(action)

            Gdx.app.log(STATE_TAG, "Consuming action $action (which takes us from $oldState to $newState)")
            currentState = newState
        }
    }

    override fun render(delta: Float) {
        var toRender: UiState? = null

        synchronized(stateLock) {
            if (renderedState !== currentState) {
                Gdx.app.log(STATE_TAG, "Rendering state $currentState (previous was $renderedState)")
                toRender = currentState
                renderedState = currentState
            }
        }

        val new = toRender
        if (new != null) {
            when(new) {
                is Splash -> showSplash()
                is ConnectingToServer -> showConnectingToServer()
                is StartingServer -> showStartingServer()
                is ClientReady -> showClientReady(new.players)
                is ServerReadyToStart -> showServerReadyToStart(new.players)
                is ServerWaitingForClients -> showServerWaitingForClients(new.me)
                is CountdownToGame -> showCountdownToGame()
                is LaunchingGame -> game.launchGame(new.gameDetails)
            }
        }

        super.render(delta)
    }

    private fun showSplash() {

        wrapper.clear()

        val description = Label("Play with others\non the same local network", game.uiAssets.getStyles().label.medium)
        description.setAlignment(Align.center)

        wrapper.add(description).colspan(2).spaceBottom(UI_SPACE)

        wrapper.row()

        wrapper.add(
            makeButton("Start server", styles) {
                changeState(Action.AttemptToStartServer())

                GlobalScope.launch(Dispatchers.IO) {
                    RetrowarsServer.start()
                    createClient(true)

                    // Don't change the state here. Instead, we will wait for a 'players updated'
                    // event from our client which will in turn trigger the appropriate state change.
                }
            }
        )

        wrapper.row()

        wrapper.add(makeButton("Join server", styles) {
            changeState(Action.AttemptToJoinServer())

            GlobalScope.launch(Dispatchers.IO) {
                createClient(false)

                // Don't change the state here. Instead, we will wait for a 'players updated'
                // event from our client which will in turn trigger the appropriate state change.
            }
        })

    }

    private fun showConnectingToServer() {
        wrapper.clear()

        wrapper.add(Label("Connecting to server", styles.label.medium))
    }

    private fun showStartingServer() {
        wrapper.clear()

        wrapper.add(Label("Starting server", styles.label.medium))
    }

    private fun showClientReady(players: List<Player>) {
        wrapper.clear()

        wrapper.add(Label("Waiting for server to start", styles.label.medium))

        wrapper.row()

        wrapper.add(makeAvatarTiles(players))
    }

    private fun showServerReadyToStart(players: List<Player>) {
        wrapper.clear()

        wrapper.add(Label("Ready to start", styles.label.medium))

        wrapper.row()

        wrapper.add(makeButton("Start game", styles) {
            RetrowarsServer.get()?.startGame()
        })

        wrapper.row()

        wrapper.add(makeAvatarTiles(players))
    }

    private fun showServerWaitingForClients(me: Player) {
        wrapper.clear()

        wrapper.add(Label("Waiting for clients to connect", styles.label.medium))

        wrapper.row()

        wrapper.add(makeAvatarTiles(listOf(me)))
    }

    private fun makeAvatarTiles(players: List<Player>) = Table().apply {

        val uiAssets = game.uiAssets

        pad(UI_SPACE)

        row().space(UI_SPACE).pad(UI_SPACE)

        val myAvatar = Avatar(players[0], uiAssets)

        // TODO: If they were not previously rendered, add this action.
        // myAvatar.addAction(CustomActions.bounce())

        add(myAvatar)
        add(makeGameIcon(players[0].getGameDetails(), uiAssets))
        add(Label("You", uiAssets.getStyles().label.large))

        if (players.size > 1) {

            players.subList(1, players.size).forEach { player ->

                row().space(UI_SPACE).pad(UI_SPACE)

                val avatar = Avatar(player, uiAssets)


                // TODO: If they were not previously rendered, add this action.
                // avatar.addAction(CustomActions.bounce())

                add(avatar)
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

    private fun showCountdownToGame() {
        val gameDetails = RetrowarsClient.get()?.me()?.getGameDetails()
        if (gameDetails == null) {
            // TODO: Handle this better
            Gdx.app.error(TAG, "Unable to figure out which game to start.")
            game.showMainMenu()
            return
        }

        wrapper.clear()

        var count = 5

        // After some experimentation, it seems the only way to get this label to animate from
        // within a table is to wrap it in a Container with isTransform = true (and don't forget
        // to enable GL_BLEND somewhere for the alpha transitions).
        val countdown = Label(count.toString(), game.uiAssets.getStyles().label.huge)
        val countdownContainer = Container(countdown).apply { isTransform = true }

        wrapper.add(countdownContainer).center().expand()

        countdownContainer.addAction(
            Actions.sequence(

                Actions.repeat(
                    count,
                    Actions.parallel(
                        Actions.run {
                            countdown.setText((count).toString())
                            count--
                        },
                        Actions.sequence(
                            Actions.alpha(0f, 0f), // Start at 0f alpha (hence duration 0f)...
                            Actions.alpha(1f, 0.4f) // ... and animate to 1.0f quite quickly.
                        ),
                        Actions.sequence(
                            Actions.scaleTo(
                                3f,
                                3f,
                                0f
                            ), // Start at 3x size (hence duration 0f)...
                            Actions.scaleTo(1f, 1f, 0.75f) // ... and scale back to normal size.
                        ),
                        Actions.delay(1f) // The other actions finish before the full second is up. Therefore ensure we show the counter for a full second before continuing.
                    )
                ),

                Actions.run {
                    changeState(Action.CountdownComplete(gameDetails))
                }
            )
        )

    }

    private fun createClient(isAlsoServer: Boolean): RetrowarsClient {
        val client = RetrowarsClient.connect(isAlsoServer)
        listenToClient(client)
        return client
    }

    // TODO: These listeners should really be added before we start to connect.
    private fun listenToClient(client: RetrowarsClient) {
        Gdx.app.log(TAG, "Listening to just start game or network close listener from client.")
        client.listen(
            startGameListener = { changeState(Action.BeginGame())},
            networkCloseListener = { wasGraceful -> game.showNetworkError(game, wasGraceful) },
            playersChangedListener = { players -> changeState(Action.PlayersChanged(players))},
        )
    }

    override fun dispose() {
        super.dispose()

        Gdx.app.log(TAG, "Disposing multiplayer lobby, will ask stage to dispose itself.")
        stage.dispose()
    }

}

sealed class Action {
    class AttemptToStartServer: Action()
    class PlayersChanged(val players: List<Player>): Action()

    class AttemptToJoinServer: Action()

    class BeginGame: Action()
    class CountdownComplete(val gameDetails: GameDetails) : Action()
}

interface UiState {
    fun consumeAction(action: Action): UiState
}

class Splash: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.AttemptToStartServer -> StartingServer()
            is Action.AttemptToJoinServer -> ConnectingToServer()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class StartingServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> ServerWaitingForClients(action.players[0])
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ServerWaitingForClients(val me: Player): UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> if (action.players.size == 1) this else ServerReadyToStart(action.players)
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ServerReadyToStart(val players: List<Player>) : UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> ServerReadyToStart(action.players)
            is Action.BeginGame -> CountdownToGame()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ConnectingToServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> ClientReady(action.players)
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ClientReady(val players: List<Player>) : UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> ClientReady(action.players)
            is Action.BeginGame -> CountdownToGame()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class CountdownToGame: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.CountdownComplete -> LaunchingGame(action.gameDetails)
            is Action.BeginGame -> CountdownToGame()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class LaunchingGame(val gameDetails: GameDetails): UiState {
    override fun consumeAction(action: Action): UiState {
        // This is a terminal state, which will cause us to leave this screen.
        throw IllegalStateException("Invalid action $action passed to state $this")
    }
}
