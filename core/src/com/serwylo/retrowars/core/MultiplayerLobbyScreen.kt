package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.net.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlin.system.measureTimeMillis


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
    private var currentState: UiState
    private var renderedState: UiState? = null

    init {
        stage.addActor(makeStageDecoration())

        val client = RetrowarsClient.get()

        currentState = if (client != null) {

            listenToClient(client)

            // A bit of a hack, but we want to use the same business logic that the WaitingForAllToReturnToLobby
            // uses to decide whether or not we are waiting for players to return, or ready to start. Therefore,
            // start with the premise we are waiting, and then provide a list of players and see what that state
            // transition logic has to say about the matter.
            WaitingForAllToReturnToLobby(client.players).consumeAction(Action.PlayersChanged(client.players))

        } else {

            Splash()

        }
    }

    private fun makeStageDecoration(): Table {
        return Table().apply {
            setFillParent(true)
            pad(UI_SPACE)

            val heading = makeHeading(strings["multiplayer-lobby.title"], styles, strings) {
                GlobalScope.launch {
                    Gdx.app.log(TAG, "Returning from lobby to main screen. Will close off any server and/or client connection.")
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
                is SearchingForPublicServers -> showSearchingForPublicServers()
                is SearchingForLocalServer -> showSearchingForLocalServer()
                is ShowingServerList -> showServerList(new.activeServers, new.pendingServers)
                is ShowEmptyServerList -> showEmptyServerList()
                is NoLocalServerFound -> showNoLocalServerFound()
                is ConnectingToServer -> showConnectingToServer()
                is StartingServer -> showStartingServer()
                is ReadyToStart -> showReadyToStart(new.players, new.previousPlayers)
                is WaitingForOtherPlayers -> showServerWaitingForClients(new.me)
                is WaitingForAllToReturnToLobby -> showServerWaitingForAllToReturnToLobby(new.players)
                is CountdownToGame -> showCountdownToGame()
                is LaunchingGame -> game.launchGame(new.gameDetails)
            }
        }

        super.render(delta)
    }

    private fun showSearchingForPublicServers() {
        wrapper.clear()

        wrapper.add(Label("Looking for public servers...", styles.label.medium))
    }

    private fun showEmptyServerList() {
        wrapper.clear()

        wrapper.add(Label("No servers found", styles.label.large))
        wrapper.row().spaceTop(UI_SPACE * 2)
        wrapper.add(Label("Want to help the Super Retro Mega Wars project\nby running a public server?", styles.label.medium).apply {
            setAlignment(Align.center)
        })
        wrapper.row().spaceTop(UI_SPACE * 2)
        wrapper.add(
            makeButton("Learn how to help", styles) {
                Gdx.net.openURI("http://github.com/retrowars/retrowars#running-a-public-server")
            }
        )
    }

    private fun showServerList(activeServers: List<ServerDetails>, pendingServers: List<ServerMetadataDTO>) {
        Gdx.app.log(TAG, "Rendering server list of ${activeServers.size} active servers and ${pendingServers.size} pending servers.")
        wrapper.clear()

        wrapper.row().spaceBottom(UI_SPACE * 2)

        wrapper.row()
        wrapper.add(
            ScrollPane(Table().apply {
                activeServers.onEach { server ->
                    row()
                    add(makeServerInfo(server)).pad(UI_SPACE).fillX()
                }
            }).apply {
                setScrollingDisabled(true, false)
            }
        ).expandY().fillY()

        if (pendingServers.isNotEmpty()) {
            wrapper.row()
            wrapper.add(
                Label(
                    "Checking ${pendingServers.size} servers:\n${pendingServers.joinToString("\n") { it.hostname }}",
                    styles.label.small
                ).apply {
                    setAlignment(Align.center)
                    addAction(
                        repeat(
                            RepeatAction.FOREVER,
                                sequence(
                                    alpha(0f),
                                    alpha(1f, 0.3f),
                                    delay(0.5f),
                                    alpha(0f, 0.3f),
                                )
                        )
                    )
                }
            )
        }

        wrapper.row().spaceTop(UI_SPACE * 2)
        wrapper.add(Label("Want to help the Super Retro Mega Wars project\nby running a public server?", styles.label.small).apply {
            setAlignment(Align.center)
        })
        wrapper.row().spaceTop(UI_SPACE)
        wrapper.add(
            makeSmallButton("Learn how to help", styles) {
                Gdx.net.openURI("http://github.com/retrowars/retrowars#running-a-public-server")
            }
        )
    }

    private fun makeServerInfo(server: ServerDetails): Actor {
        val styles = game.uiAssets.getStyles()
        val skin = game.uiAssets.getSkin()
        return Table().apply {
            background = skin.getDrawable("window")
            padTop(UI_SPACE)
            padBottom(UI_SPACE)
            padLeft(UI_SPACE * 2)
            padRight(UI_SPACE * 2)

            add(
                Label(
                    server.hostname,
                    styles.label.medium
                )
            ).expandX().colspan(2).spaceBottom(UI_SPACE).left()

            row()

            val metadata = Table()

            metadata.add(Label("Rooms:", styles.label.small)).right()
            metadata.add(Label(server.currentRoomCount.toString(), styles.label.small)).left().padLeft(UI_SPACE)
            metadata.row()

            metadata.add(Label("Players:", styles.label.small)).right()
            metadata.add(Label(server.currentPlayerCount.toString(), styles.label.small)).left().padLeft(UI_SPACE)
            metadata.row()

            metadata.add(Label("Last game:", styles.label.small)).right()
            metadata.add(Label(roughTimeAgo(server.lastGameTimestamp), styles.label.small)).left().padLeft(UI_SPACE)
            metadata.row()

            metadata.add(Label("Fetched info in:", styles.label.small)).right()
            metadata.add(Label("${server.pingTime}ms", styles.label.small)).left().padLeft(UI_SPACE)
            metadata.row()

            val infoCell:Cell<Actor> = add().left().top().spaceRight(UI_SPACE * 2)
            infoCell.setActor(
                VerticalGroup().apply {
                    addActor(Label(calcServerActivitySummary(server), styles.label.small))
                    addActor(
                        makeSmallButton("View info", styles) {
                            infoCell.clearActor()
                            infoCell.setActor(metadata)
                        }
                    )
                    space(UI_SPACE / 2f)
                    columnAlign(Align.left)
                }
            )

            add(
                makeButton("Join", styles) {
                    changeState(Action.AttemptToJoinServer)

                    GlobalScope.launch(Dispatchers.IO) {
                        createClient(server.hostname, server.port)
                    }
                }.apply {
                    padLeft(UI_SPACE * 2)
                    padRight(UI_SPACE * 2)
                }
            ).expandX().right().bottom()
        }
    }

    private fun calcServerActivitySummary(server: ServerDetails): String {
        if (server.lastGameTimestamp >= 0 && server.lastGameTimestamp < 10 * 1000 * 60 /* 10 minutes */) {
            if (server.currentRoomCount > 1) {
                return "Very active"
            } else if (server.currentPlayerCount > 2) {
                return "Somewhat active"
            }
        }

        return "Not very active"
    }

    private fun roughTimeAgo(timestamp: Long): String {
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

    private fun showSplash() {

        wrapper.clear()

        wrapper.add(
            Label("Play on your WiFi network", game.uiAssets.getStyles().label.medium).apply {
                setAlignment(Align.center)
            }
        ).colspan(2).spaceBottom(UI_SPACE)

        wrapper.row()

        wrapper.add(
            makeButton("Start local server", styles) {
                changeState(Action.AttemptToStartServer)

                GlobalScope.launch(Dispatchers.IO) {
                    RetrowarsServer.start(game.platform)
                    createClient("localhost", Network.defaultPort)

                    // Don't change the state here. Instead, we will wait for a 'players updated'
                    // event from our client which will in turn trigger the appropriate state change.
                }
            }
        )

        wrapper.add(
            makeButton("Join local server", styles) {
                findAndJoinLocalServer()
            }
        )

        wrapper.row().spaceTop(UI_SPACE * 4)

        wrapper.add(
            Label("Play other retro fans\nover the internet", game.uiAssets.getStyles().label.medium).apply {
                setAlignment(Align.center)
            }
        ).colspan(2).spaceBottom(UI_SPACE)

        wrapper.row()

        wrapper.add(
            makeButton("Find a public server", styles) {
                GlobalScope.launch {
                    findAndShowPublicServers()
                }
            }
        ).colspan(2)

        wrapper.row()

    }

    private fun findAndJoinLocalServer() {

        changeState(Action.FindLocalServer)

        game.platform.getMulticastControl().acquireLock()

        // Once we've found a server, we will ask jmdns to close. However it is likely it may still
        // find other servers in the meantime. Therefore, lets guard against this by ignoring any
        // other servers after serverFound is set to true.
        var serverFound = false

        // TODO: If time out occurs, change to a view showing: "Could not server on the local network to connect to."
        //       Also use the timeout to trigger the release of the multicast lock.
        val jmdns = JmDNS.create(game.platform.getInetAddress())

        jmdns.addServiceListener(Network.jmdnsServiceName, object: ServiceListener {

            override fun serviceAdded(event: ServiceEvent?) {
                synchronized(serverFound) {
                    if (serverFound) {
                        Gdx.app.debug(TAG, "Retrowars server has already been found, so disregarding \"service added\" event: $event")
                        return
                    }

                    // TODO: serviceAdded() is an intermediate step before being resolved.
                    //       It may be possible to provide more fine grained feedback here while
                    //       we wait for the full resolution.
                    Gdx.app.log(TAG, "Found service: $event")
                    jmdns.requestServiceInfo(Network.jmdnsServiceName, event?.name)
                }
            }

            override fun serviceRemoved(event: ServiceEvent?) {}

            override fun serviceResolved(event: ServiceEvent?) {
                synchronized(serverFound) {
                    if (serverFound) {
                        Gdx.app.debug(TAG, "Retrowars server has already been found, so disregarding \"service resolved\" event: $event")
                        return
                    }

                    serverFound = true
                }

                Gdx.app.log(TAG, "Resolved service: $event")
                val info = event?.info

                if (info == null) {
                    Gdx.app.error(TAG, "Resolved retrowars server via jmdns, but couldn't get any info about it. Will ignore.")
                    return
                }

                if (info.inet4Addresses.isEmpty()) {
                    Gdx.app.error(TAG, "Resolved retrowars server via jmdns, but no IP addresses were present, this is weird and unexpected, will ignore.")
                    return
                }

                val port = info.port
                val host = info.inet4Addresses[0]

                Gdx.app.debug(TAG, "Found local retrowars server at $host:$port")

                // Fire and forget this, we don't want that to stop us from actually connecting to
                // the client.
                GlobalScope.launch(Dispatchers.IO) {
                    runCatching {
                        Gdx.app.debug(TAG, "Closing jmdns as we found the details we were looking for.")
                        game.platform.getMulticastControl().releaseLock()
                        jmdns.close()
                        Gdx.app.debug(TAG, "Finished closing jmdns.")
                    }
                }

                Gdx.app.debug(TAG, "Creating client connection to ${host.hostAddress}:$port")
                changeState(Action.AttemptToJoinServer)
                createClient(host.hostAddress, port)

                // Don't change the state here. Instead, we will wait for a 'players updated'
                // event from our client which will in turn trigger the appropriate state change.
            }
        })

        GlobalScope.launch(Dispatchers.IO) {
            Gdx.app.debug(TAG, "Waiting 10 seconds before timing out after searching for a server.")
            delay(10000)

            Gdx.app.debug(TAG, "10 seconds is up, checking if we found a server.")
            synchronized(serverFound) {
                if (serverFound) {
                    // Great, we found a server, so we can assume we've already closed of jmdns
                    // (or we are in the process of closing it).
                    Gdx.app.debug(TAG, "Timeout unnecessary, because we found a server. Will cancel coroutine and not bother closing off JmDNS.")
                    cancel()
                    return@launch
                }

                // Set this to true, so that if in the same time that we are trying to close off
                // JmDNS another response comes in, it is ignored. Too late, you had your chance.
                serverFound = true
            }

            Gdx.app.log(TAG, "Unable to find local server after 10s, so cancelling jmdns and notifying user.")
            changeState(Action.UnableToFindLocalServer)
            runCatching {
                game.platform.getMulticastControl().releaseLock()
                jmdns.close()
            }
        }

    }

    private suspend fun findAndShowPublicServers() = withContext(Dispatchers.IO) {
        changeState(Action.FindPublicServers)

        val allServers = fetchPublicServerList()

        // Take a copy so we can iterate over allServers, but mutate pendingServers.
        var pendingServers = allServers.toList()
        yield()

        var activeServers = listOf<ServerDetails>()
        var inactiveServers = listOf<ServerMetadataDTO>()

        val update = {
            changeState(Action.ShowPublicServers(activeServers, pendingServers, inactiveServers))
        }

        update()

        data class ServerInfoResult(val server: ServerMetadataDTO, val info: ServerInfoDTO?, val pingTime: Long)

        val serverInfoChannel = Channel<ServerInfoResult>()

        val numServers = pendingServers.size

        allServers.onEach { server ->
            launch {
                Gdx.app.debug(TAG, "Fetching server metadata for ${server.hostname}")
                val info: ServerInfoDTO?
                val pingTime = measureTimeMillis {
                    info = fetchServerInfo(server)
                }

                serverInfoChannel.send(ServerInfoResult(server, info, pingTime))
            }
        }

        for (i in 0 until numServers) {
            val result = serverInfoChannel.receive()

            val info = result.info
            val server = result.server
            val pingTime = result.pingTime

            if (info == null) {

                Gdx.app.log(TAG, "Showing server ${server.hostname} as inactive.")
                synchronized(this) {
                    inactiveServers = inactiveServers.plus(server)
                }


                // Right now we don't yet support private rooms (they will require an invite mechanism to work).
            } else if (info.type == "publicRandomRooms") {

                Gdx.app.log(TAG, "Found stats for ${server.hostname} [rooms: ${info.currentRoomCount}, players: ${info.currentPlayerCount}, last game: ${info.lastGameTimestamp}].")
                activeServers = activeServers.plus(ServerDetails(
                    server.hostname,
                    server.port,
                    info.type,
                    info.maxPlayersPerRoom,
                    info.maxRooms,
                    info.currentRoomCount,
                    info.currentPlayerCount,
                    info.lastGameTimestamp,
                    pingTime.toInt(),
                )).sortedBy { serverDetails ->
                    // We could sort by ping time, but it just isn't the only relevant metric here.
                    // Equally we could sort by most active servers first, but again, may not be ideal.
                    // As such, lets just let the authors of the server metadata file decide on the order.
                    allServers.map { it.hostname }.indexOf(serverDetails.hostname)
                }
            }

            Gdx.app.debug(TAG, "Updating list of servers with new information about ${server.hostname}")
            pendingServers = pendingServers.filter { it !== server }

            update()
        }
    }

    private fun showSearchingForLocalServer() {
        wrapper.clear()

        wrapper.add(Label("Searching for server", styles.label.medium))
        wrapper.row()
        wrapper.add(
            Label("Make sure you're both connected to the same WiFi network", styles.label.small).apply {
                addAction(
                    sequence(
                        alpha(0f, 0f), // Start at 0f alpha (hence duration 0f)...
                        delay(2f), // ...after the player has had to wait for a few seconds...
                        alpha(1f, 1f), // ...show them this message as a prompt.
                    )
                )
            }
        )
    }

    private fun showNoLocalServerFound() {
        wrapper.clear()

        wrapper.add(Label("Could not find server", styles.label.medium))
        wrapper.row()
        wrapper.add(Label("Has another player started a server?", styles.label.small))
        wrapper.row()
        wrapper.add(Label("Are you on the same WiFi network?", styles.label.small))
        wrapper.row()
        wrapper.add(Label("Do you both have the latest version of Super Retro Mega Wars?", styles.label.small))
    }

    private fun showConnectingToServer() {
        wrapper.clear()

        wrapper.add(Label("Connecting to server", styles.label.medium))
    }

    private fun showStartingServer() {
        wrapper.clear()

        wrapper.add(Label("Starting server", styles.label.medium))
    }

    private fun showReadyToStart(players: List<Player>, previousPlayers: List<Player>) {
        wrapper.clear()

        wrapper.add(Label("Ready to start", styles.label.medium))

        wrapper.row().spaceTop(UI_SPACE)

        wrapper.add(makeButton("Start game", styles) {
            RetrowarsClient.get()?.startGame()
        })

        wrapper.row()

        wrapper.add(makeAvatarTiles(players, previousPlayers))
    }

    private fun showServerWaitingForClients(me: Player) {
        wrapper.clear()

        wrapper.add(Label("Waiting for other players to join", styles.label.medium))

        wrapper.row()

        wrapper.add(makeAvatarTiles(listOf(me), listOf()))
    }

    private fun showServerWaitingForAllToReturnToLobby(players: List<Player>) {
        wrapper.clear()

        wrapper.add(Label("Waiting for everyone to return to lobby", styles.label.medium))

        wrapper.row()

        wrapper.add(makeAvatarTiles(players, listOf()))

        wrapper.row()
    }

    private fun makeAvatarTiles(players: List<Player>, previousPlayers: List<Player>) = Table().apply {

        val uiAssets = game.uiAssets

        pad(UI_SPACE)

        row().space(UI_SPACE)

        val myAvatar = Avatar(players[0], uiAssets)

        if (!previousPlayers.any { it.id == players[0].id }) {
            myAvatar.addAction(CustomActions.bounce())
        }

        add(myAvatar)
        add(makeGameIcon(players[0].getGameDetails(), uiAssets))
        add(Label("You", uiAssets.getStyles().label.large))

        if (players.size > 1) {

            players.subList(1, players.size).forEach { player ->

                row().space(UI_SPACE)

                val avatar = Avatar(player, uiAssets)

                if (!previousPlayers.any { it.id == player.id }) {
                    avatar.addAction(CustomActions.bounce())
                }

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
            sequence(

                repeat(
                    count,
                    parallel(
                        Actions.run {
                            countdown.setText((count).toString())
                            count--
                        },
                        sequence(
                            alpha(0f, 0f), // Start at 0f alpha (hence duration 0f)...
                            alpha(1f, 0.4f) // ... and animate to 1.0f quite quickly.
                        ),
                        sequence(
                            scaleTo(
                                3f,
                                3f,
                                0f
                            ), // Start at 3x size (hence duration 0f)...
                            scaleTo(1f, 1f, 0.75f) // ... and scale back to normal size.
                        ),
                        delay(1f) // The other actions finish before the full second is up. Therefore ensure we show the counter for a full second before continuing.
                    )
                ),

                Actions.run {
                    changeState(Action.CountdownComplete(gameDetails))
                }
            )
        )

    }

    private fun createClient(host: String, port: Int): RetrowarsClient {
        val client = RetrowarsClient.connect(host, port)
        listenToClient(client)
        return client
    }

    // TODO: These listeners should really be added before we start to connect.
    private fun listenToClient(client: RetrowarsClient) {
        Gdx.app.log(TAG, "Listening to start game, network close, or player change related events from the server.")
        client.listen(
            startGameListener = { changeState(Action.BeginGame)},
            networkCloseListener = { wasGraceful -> game.showNetworkError(game, wasGraceful) },
            playersChangedListener = { players -> changeState(Action.PlayersChanged(players))},
            playerStatusChangedListener = { _, _ -> changeState(Action.PlayersChanged(RetrowarsClient.get()?.players ?: listOf())) },
        )
    }

    override fun dispose() {
        super.dispose()

        Gdx.app.log(TAG, "Disposing multiplayer lobby, will ask stage to dispose itself.")
        stage.dispose()
    }

}

sealed class Action {
    object AttemptToStartServer : Action()
    object FindPublicServers : Action()
    object FindLocalServer : Action()
    class ShowPublicServers(val activeServers: List<ServerDetails>, val pendingServers: List<ServerMetadataDTO>, val inactiveServers: List<ServerMetadataDTO>) : Action()
    class PlayersChanged(val players: List<Player>): Action()

    object AttemptToJoinServer : Action()
    object UnableToFindLocalServer : Action()

    object BeginGame : Action()
    class CountdownComplete(val gameDetails: GameDetails) : Action()
}

interface UiState {
    fun consumeAction(action: Action): UiState

    fun unsupported(action: Action): Nothing {
        throw IllegalStateException("Invalid action $action passed to state $this")
    }
}

class Splash: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.AttemptToStartServer -> StartingServer()
            is Action.FindPublicServers -> SearchingForPublicServers()
            is Action.FindLocalServer -> SearchingForLocalServer()
            else -> unsupported(action)
        }
    }
}

class StartingServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> WaitingForOtherPlayers(action.players[0])
            else -> unsupported(action)
        }
    }
}

class WaitingForOtherPlayers(val me: Player): UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> if (action.players.size == 1) this else ReadyToStart(action.players, listOf(me))
            else -> unsupported(action)
        }
    }
}

class SearchingForPublicServers: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ShowPublicServers ->
                if (action.activeServers.isEmpty() && action.pendingServers.isEmpty()) {
                    ShowEmptyServerList()
                } else {
                    ShowingServerList(action.activeServers, action.pendingServers, action.inactiveServers)
                }
            else -> unsupported(action)
        }
    }
}

class SearchingForLocalServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.AttemptToJoinServer -> ConnectingToServer()
            is Action.UnableToFindLocalServer -> NoLocalServerFound()
            else -> unsupported(action)
        }
    }
}

class ShowingServerList(val activeServers: List<ServerDetails>, val pendingServers: List<ServerMetadataDTO>, val inactiveServers: List<ServerMetadataDTO>): UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ShowPublicServers ->
                if (action.activeServers.isEmpty() && action.pendingServers.isEmpty()) {
                    ShowEmptyServerList()
                } else {
                    ShowingServerList(action.activeServers, action.pendingServers, action.inactiveServers)
                }

            is Action.AttemptToJoinServer -> ConnectingToServer()
            else -> unsupported(action)
        }
    }
}

class ShowEmptyServerList: UiState {
    override fun consumeAction(action: Action): UiState {
        // User needs to press "Back" from the top menu to go to the main menu and start again.
        unsupported(action)
    }
}

/**
 * @param previousPlayers Given that we can regularly update the list of players, we record the previous
 *                        set of players which were shown so that we can animate new players coming into
 *                        the screen.
 */
class ReadyToStart(val players: List<Player>, val previousPlayers: List<Player>) : UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> ReadyToStart(action.players, this.players)
            is Action.BeginGame -> CountdownToGame()
            else -> unsupported(action)
        }
    }
}

class WaitingForAllToReturnToLobby(val players: List<Player>) : UiState {
    override fun consumeAction(action: Action): UiState {
        if (action !is Action.PlayersChanged) {
            unsupported(action)
        }

        return when {
            action.players.size == 1 -> WaitingForOtherPlayers(action.players[0])
            action.players.any { it.status != Player.Status.lobby } -> WaitingForAllToReturnToLobby(action.players)
            else -> return ReadyToStart(action.players, this.players)
        }
    }
}

class NoLocalServerFound: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            // The user must press the "back" button to continue.
            else -> unsupported(action)
        }
    }
}

class ConnectingToServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged -> if (action.players.size == 1) WaitingForOtherPlayers(action.players[0]) else ReadyToStart(action.players, listOf())
            else -> unsupported(action)
        }
    }
}

class CountdownToGame: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.CountdownComplete -> LaunchingGame(action.gameDetails)
            is Action.BeginGame -> CountdownToGame()
            else -> unsupported(action)
        }
    }
}

class LaunchingGame(val gameDetails: GameDetails): UiState {
    override fun consumeAction(action: Action): UiState {
        // This is a terminal state, which will cause us to leave this screen.
        unsupported(action)
    }
}
