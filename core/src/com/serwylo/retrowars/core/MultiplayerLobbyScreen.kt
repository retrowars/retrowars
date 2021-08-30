package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.net.*
import com.serwylo.retrowars.ui.createPlayerSummaries
import com.serwylo.retrowars.ui.roughTimeAgo
import com.serwylo.retrowars.utils.AppProperties
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
                networkCloseListener = { _, _ -> }
            )

            // If we are running a local server, then stop it.
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

    private val findPublicServersJob = Job()
    private val findPublicServersScope = CoroutineScope(Dispatchers.IO + findPublicServersJob)

    init {
        stage.addActor(makeStageDecoration())

        val client = RetrowarsClient.get()

        currentState = if (client != null) {

            val state = when {
                client.players.any { it.status == Player.Status.playing } -> ObservingGameInProgress(client.me()!!, client.scores)
                client.players.any { it.status == Player.Status.dead } -> FinalScores(client.me()!!, client.scores)
                else -> ReadyToStart(client.players, listOf())
            }

            listenToClient(client)

            state

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
                is ShowingServerList -> showServerList(new.activeServers, new.pendingServers, new.unsupportedServers)
                is ShowEmptyServerList -> showEmptyServerList()
                is NoLocalServerFound -> showNoLocalServerFound()
                is ConnectingToServer -> showConnectingToServer()
                is StartingServer -> showStartingServer()
                is ReadyToStart -> showReadyToStart(new.players, new.previousPlayers)
                is WaitingForOtherPlayers -> showServerWaitingForClients(new.me)
                is ObservingGameInProgress -> showObservingGameInProgress(new.me, new.scores)
                is FinalScores -> showFinalScores(new.me, new.scores)
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
                Gdx.net.openURI("https://github.com/retrowars/retrowars-servers/#contributing")
            }
        )
    }

    private fun showServerList(
        activeServers: List<ServerDetails>,
        pendingServers: List<ServerMetadataDTO>,
        unsupportedServers: List<ServerDetails>,
    ) {
        Gdx.app.log(TAG, "Rendering server list of ${activeServers.size} active servers and ${pendingServers.size} pending servers.")
        wrapper.clear()

        wrapper.row()
        wrapper.add(
            ScrollPane(Table().apply {
                activeServers.onEach { server ->
                    row()
                    add(makeServerInfo(server)).pad(UI_SPACE).expandX().fillX()
                }

                unsupportedServers.onEach { server ->
                    row()
                    add(makeUnsupportedServerInfo(server)).pad(UI_SPACE).expandX().fillX()
                }
            }).apply {
                setScrollingDisabled(true, false)
            }
        ).expandY().fillY().fillX()

        wrapper.row()
        wrapper.add(Label("v${AppProperties.appVersionName}", styles.label.small)).right()

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
                                    alpha(1f),
                                    delay(1f),
                                    alpha(0f, 0.5f),
                                    delay(0.5f),
                                    alpha(1f, 0.5f),
                                )
                        )
                    )
                }
            )
        }

        wrapper.row().spaceTop(UI_SPACE * 2)
        wrapper.add(Label("Want to help the Super Retro Mega Wars community\nby running a public server?", styles.label.small).apply {
            setAlignment(Align.center)
        })
        wrapper.row().spaceTop(UI_SPACE)
        wrapper.add(
            makeSmallButton("Learn how to help", styles) {
                Gdx.net.openURI("https://github.com/retrowars/retrowars-servers/#contributing")
            }
        )
    }

    private fun makeUnsupportedServerInfo(server: ServerDetails): Actor {

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
            add(Label("Unsupported.\nPlease upgrade to at least v${AppProperties.appVersionName}.", styles.label.small)).top().left()
            add(
                makeButton("Join", styles) {}.apply {
                    isDisabled = true
                    touchable(Touchable.disabled)
                    padLeft(UI_SPACE * 2)
                    padRight(UI_SPACE * 2)
                }
            ).expandX().right().bottom()
        }
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

            metadata.add(Label("Version:", styles.label.small)).right()
            metadata.add(Label("v${server.versionName}", styles.label.small)).left().padLeft(UI_SPACE)
            metadata.row()

            metadata.add(Label("Fetched info in:", styles.label.small)).right()
            metadata.add(Label("${server.pingTime}ms", styles.label.small)).left().padLeft(UI_SPACE)
            metadata.row()

            val infoCell:Cell<Actor> = add().left().top().spaceRight(UI_SPACE * 2)
            infoCell.setActor(
                VerticalGroup().apply {
                    if (server.minSupportedClientVersionCode < AppProperties.appVersionCode) {
                        addActor(Label(calcServerActivitySummary(server), styles.label.small))
                    }
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
                    Gdx.app.debug(TAG, "About to join server ${server.hostname}:${server.port}. Will cancel the job scheduled to find all servers in case there are any still in progress (no longer relevant now we have selected as server).")
                    findPublicServersJob.cancel()
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
        if (server.lastGameTimestamp >= 0 && server.lastGameTimestamp < 60 * 1000 * 60 /* 1hr */) {
            return when {
                server.currentRoomCount > 1 -> "Very active"
                server.currentPlayerCount > 1 -> "Active"
                else -> "Recently active"
            }
        }

        return "Not very active"
    }

    private fun showSplash() {

        wrapper.clear()

        wrapper.add(
            Label("Play other retro fans\nover the internet", game.uiAssets.getStyles().label.medium).apply {
                setAlignment(Align.center)
            }
        ).colspan(2).spaceBottom(UI_SPACE)

        wrapper.row()

        wrapper.add(
            makeLargeButton("Play online", styles) {
                findPublicServersScope.launch {
                    findAndShowPublicServers()
                }
            }
        ).colspan(2)

        wrapper.row().spaceTop(UI_SPACE * 4)

        wrapper.add(
            Label("Play friends on your WiFi network", game.uiAssets.getStyles().label.medium).apply {
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
        var unsupportedServers = listOf<ServerDetails>()

        val update = {
            changeState(Action.ShowPublicServers(activeServers, pendingServers, inactiveServers, unsupportedServers))
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
                    info = try {
                        fetchServerInfo(server)
                    } catch (e: Exception) {
                        Gdx.app.error(TAG, "Could not fetch server metadata from ${server.hostname}:${server.port}. Will ignore it.", e)
                        null
                    }
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
                inactiveServers = inactiveServers.plus(server)

            } else if (info.minSupportedClientVersionCode > AppProperties.appVersionCode) {

                try {
                    unsupportedServers = unsupportedServers.plus(ServerDetails(
                        server.hostname,
                        server.port,
                        info.versionCode,
                        info.versionName,
                        info.minSupportedClientVersionCode,
                        info.minSupportedClientVersionName,
                        info.type,
                        info.maxPlayersPerRoom,
                        info.maxRooms,
                        info.currentRoomCount,
                        info.currentPlayerCount,
                        info.lastGameTimestamp,
                        pingTime.toInt(),
                    ))
                } catch (e: Exception) {
                    Gdx.app.error(TAG, "Error getting server: ${e.message}", e)
                    inactiveServers = inactiveServers.plus(server)
                }

            // Right now we don't yet support private rooms (they will require an invite mechanism to work).
            } else if (info.type == "publicRandomRooms") {

                try {
                    Gdx.app.log(TAG, "Found stats for ${server.hostname} [rooms: ${info.currentRoomCount}, players: ${info.currentPlayerCount}, last game: ${info.lastGameTimestamp}].")
                    activeServers = activeServers.plus(ServerDetails(
                        server.hostname,
                        server.port,
                        info.versionCode,
                        info.versionName,
                        info.minSupportedClientVersionCode,
                        info.minSupportedClientVersionName,
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
                } catch (e: Exception) {
                    Gdx.app.error(TAG, "Error fetching details of server, will ignore: ${e.message}.")
                    inactiveServers = inactiveServers.plus(server)
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


    private fun showObservingGameInProgress(me: Player, scores: Map<Player, Long>) {
        wrapper.clear()

        wrapper.add(Label("Game in progress", styles.label.medium))
        wrapper.row()
        wrapper.add(Label("You will join the next game...", styles.label.small))
        wrapper.row()
        wrapper.add(createPlayerSummaries(me, scores, showDeaths = true, game.uiAssets))
    }

    private fun showFinalScores(me: Player, scores: Map<Player, Long>) {
        wrapper.clear()

        wrapper.add(Label("Final scores", styles.label.medium))
        wrapper.row()
        wrapper.add(Label("The next game will begin soon...", styles.label.small))
        wrapper.row()
        wrapper.add(createPlayerSummaries(me, scores, showDeaths = false, game.uiAssets))
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
                            Player.Status.pending -> "Ready"
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
            networkCloseListener = { code, message -> game.showNetworkError(code, message) },
            playersChangedListener = { players -> changeState(Action.PlayersChanged(players))},
            scoreChangedListener = { _, _ -> changeState(Action.ScoreUpdated(RetrowarsClient.get()?.scores?.toMap() ?: mapOf()))},
            playerStatusChangedListener = { _, _ -> changeState(Action.PlayersChanged(RetrowarsClient.get()?.players ?: listOf())) },
            returnToLobbyListener = { changeState(Action.ReturnToLobby(client.players)) },
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
    class ShowPublicServers(val activeServers: List<ServerDetails>, val pendingServers: List<ServerMetadataDTO>, val inactiveServers: List<ServerMetadataDTO>, val unsupportedServers: List<ServerDetails>) : Action()
    class PlayersChanged(val players: List<Player>): Action() {
        fun isPending() = players.isNotEmpty() && players[0].status == Player.Status.pending
    }
    class ScoreUpdated(val scores: Map<Player, Long>): Action()
    class ShowFinalScores(val scores: Map<Player, Long>): Action()
    class ReturnToLobby(val players: List<Player>): Action()

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
            is Action.PlayersChanged ->
                when {
                    action.players.size == 1 -> this
                    action.players.size > 1 -> ReadyToStart(action.players, listOf())
                    else -> throw IllegalStateException("Received a PlayersChanged event with no players. Should have at least myself.")
                }
            else -> unsupported(action)
        }
    }
}

class SearchingForPublicServers: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ShowPublicServers ->
                if (action.activeServers.isEmpty() && action.pendingServers.isEmpty() && action.unsupportedServers.isEmpty()) {
                    ShowEmptyServerList()
                } else {
                    ShowingServerList(action.activeServers, action.pendingServers, action.inactiveServers, action.unsupportedServers)
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

class ShowingServerList(
    val activeServers: List<ServerDetails>,
    val pendingServers: List<ServerMetadataDTO>,
    val inactiveServers: List<ServerMetadataDTO>,
    val unsupportedServers: List<ServerDetails>,
): UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.ShowPublicServers ->
                if (action.activeServers.isEmpty() && action.pendingServers.isEmpty() && action.unsupportedServers.isEmpty()) {
                    ShowEmptyServerList()
                } else {
                    ShowingServerList(
                        action.activeServers,
                        action.pendingServers,
                        action.inactiveServers,
                        action.unsupportedServers
                    )
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

class ObservingGameInProgress(val me: Player, val scores: Map<Player, Long>) : UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged ->
                if (action.players.none { it.status == Player.Status.playing }) {
                    // If no players are playing any more, go to the final scores screen. At some
                    // point in the future, we will receive a subsequent event from the server asking
                    // us to go to the lobby - but that will happen in the FinalScores state, not
                    // the ObservingGameInProgress state.
                    FinalScores(me, scores)
                } else {
                    // If a player was added, then add them to the list in their "pending" state so that
                    // we can explain to others they will join in the next game.
                    // If a player was removed, don't show their scores any more.
                    ObservingGameInProgress(me, scores.filter { action.players.contains(it.key) })
                }
            is Action.ScoreUpdated -> ObservingGameInProgress(me, action.scores)
            is Action.ShowFinalScores -> FinalScores(me, action.scores)
            else -> unsupported(action)
        }
    }
}

class FinalScores(val me: Player, val scores: Map<Player, Long>) : UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            // Make all players appear anew when we go from an end game screen to a ready-to-start screen.
            is Action.ReturnToLobby -> ReadyToStart(action.players, listOf())

            // If a player was removed, filter that player out and then display the same screen again.
            // If a player was added, just ignore it and display the same list of scores we already had - the new
            // player obviously wasn't part of the last game, so no need to display their scores here.
            is Action.PlayersChanged -> FinalScores(me, scores.filter { score -> action.players.any { player -> score.key.id == player.id } })

            // Sometimes this event will come through when the final scores are already being shown.
            // Perhaps a race condition?
            // TODO: Find out why this is the case and ensure that once we've been directed to the
            //       FinalScores screen that we never send a ScoreUpdated even afterwards.
            is Action.ScoreUpdated -> this

            else -> unsupported(action)
        }
    }
}

class NoLocalServerFound: UiState {
    override fun consumeAction(action: Action): UiState {
        // The user must press the "back" button to continue, we don't expect any action to be
        // triggered here (as we are not connected to a network, so we can't be proactively notified
        // about any change events).
        unsupported(action)
    }
}

class ConnectingToServer: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.PlayersChanged ->
                when {
                    action.isPending() -> ObservingGameInProgress(action.players[0], mapOf())
                    action.players.size == 1 -> WaitingForOtherPlayers(action.players[0])
                    action.players.size > 1 -> ReadyToStart(action.players, action.players)
                    else -> throw IllegalStateException("Expected at least one player but got zero.")
                }
            else -> unsupported(action)
        }
    }
}

class CountdownToGame: UiState {
    override fun consumeAction(action: Action): UiState {
        return when(action) {
            is Action.CountdownComplete -> LaunchingGame(action.gameDetails)
            is Action.BeginGame -> CountdownToGame()

            // If a player joins while we are counting down, then ignore them and continue showing
            // the current state. Those players will be observing the current game so we only care
            // about them after the current game ends.
            is Action.PlayersChanged -> this

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
