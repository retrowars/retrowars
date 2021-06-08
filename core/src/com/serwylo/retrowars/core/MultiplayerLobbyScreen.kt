package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails
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

    private val wrapper: Table

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
        wrapper = makeStageDecoration()
        stage.addActor(wrapper)
        stage.isDebugAll = true

        showSplash()
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

            Gdx.app.debug(STATE_TAG, "Consuming action $action (which takes us from $oldState to $newState)")
            currentState = newState
        }
    }

    override fun render(delta: Float) {
        var toRender: UiState? = null

        synchronized(stateLock) {
            if (renderedState !== currentState) {
                Gdx.app.debug(STATE_TAG, "Rendering state $currentState (previous was $renderedState)")
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
                is ClientReady -> showClientReady()
                is ServerReadyToStart -> showServerReadyToStart()
                is ServerWaitingForClients -> showServerWaitingForClients()
                is CountdownToGame -> showCountdownToGame()
                is LaunchingGame -> game.launchGame(new.gameDetails)
            }
        }

        super.render(delta)
    }

    private fun showSplash() {

        wrapper.clear()

        wrapper.add(
            makeButton("Start server", styles) {
                changeState(Action.AttemptToStartServer())

                GlobalScope.launch(Dispatchers.IO) {
                    RetrowarsServer.start()
                    createClient(true)
                    changeState(Action.StartedServer())
                }

            }
        )

        wrapper.row()

        wrapper.add(makeButton("Join server", styles) {
            changeState(Action.AttemptToJoinServer())

            GlobalScope.launch(Dispatchers.IO) {
                createClient(false)
                changeState(Action.JoinedServer())
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

    private fun showClientReady() {
        wrapper.clear()

        wrapper.add(Label("Waiting for server to start", styles.label.medium))
    }

    private fun showServerReadyToStart() {
        wrapper.clear()

        wrapper.add(Label("Ready to start", styles.label.medium))

        wrapper.row()

        wrapper.add(makeButton("Start game", styles) {
            changeState(Action.BeginGame())
        })
    }

    private fun showServerWaitingForClients() {
        wrapper.clear()

        wrapper.add(Label("Waiting for clients to connect", styles.label.medium))
    }

    private fun showCountdownToGame() {
        val gameDetails = RetrowarsClient.get()?.me()?.getGameDetails()
        if (gameDetails == null) {
            // TODO: Handle this better
            Gdx.app.log(TAG, "Unable to figure out which game to start.")
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

    // TODO: Race condition between this and the other client.listen(...) call (after appending avatars)
    //       seems to cause a condition where you just see "Connected to server" but no information
    //       about any of the players.
    private fun listenToClient(client: RetrowarsClient) {
        Gdx.app.log(TAG, "Listening to just start game or network close listener from client.")
        client.listen(
            startGameListener = { changeState(Action.BeginGame())},
            networkCloseListener = { wasGraceful -> game.showNetworkError(game, wasGraceful) },
            playersChangedListener = { changeState(Action.ClientsUpdated())}
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
    class StartedServer: Action()
    class ClientsUpdated: Action()

    class AttemptToJoinServer: Action()
    class JoinedServer: Action()

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
            is Action.StartedServer -> ServerWaitingForClients()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ServerWaitingForClients: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ClientsUpdated -> ServerReadyToStart()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ServerReadyToStart: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ClientsUpdated -> ServerReadyToStart()
            is Action.BeginGame -> CountdownToGame()
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ConnectingToServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.JoinedServer -> ClientReady()
            is Action.ClientsUpdated -> this // We connect ourselves to the server, so we will receive an event saying we connected at some point.
            else -> throw IllegalStateException("Invalid action $action passed to state $this")
        }
    }
}

class ClientReady: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ClientsUpdated -> ClientReady()
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
