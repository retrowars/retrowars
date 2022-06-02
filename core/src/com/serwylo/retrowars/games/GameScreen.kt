package com.serwylo.retrowars.games

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.serwylo.beatgame.ui.withBackground
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.input.SoftController
import com.serwylo.retrowars.net.Network
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.scoring.Stats
import com.serwylo.retrowars.scoring.recordStats
import com.serwylo.retrowars.scoring.saveHighScore
import com.serwylo.retrowars.ui.*
import com.serwylo.retrowars.utils.Options
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class GameScreen(
    protected val game: RetrowarsGame,
    private val gameDetails: GameDetails,
    minWorldWidth: Float,
    minWorldHeight: Float,
    isOrthographic: Boolean = true,
) : Screen {

    companion object {
        const val TAG = "GameScreen"
        private const val ALLOW_SIMULATED_ATTACK = false
    }

    enum class State {
        Playing,
        Finished,
    }

    private val camera = if (isOrthographic) { OrthographicCamera() } else {
        PerspectiveCamera(67f, 1f, 1f)
    }
    protected val viewport = GameViewport(minWorldWidth, minWorldHeight, camera)
    protected val strings = game.uiAssets.getStrings()

    private val hud: HUD

    protected val controller: SoftController? = if (gameDetails.controllerLayout != null) {
        SoftController(game.uiAssets, gameDetails.controllerLayout, Options.getSoftController(gameDetails))
    } else {
        null
    }

    private val startTime = System.currentTimeMillis()

    private val client = RetrowarsClient.get()

    private var score = 0L

    /**
     * Bit of a hack, but we end up in a loop whereby we send
     */
    private var isEnding = false

    private var state = State.Playing

    /**
     * When damage is received on the network thread, queue up the players who are responsible and
     * tally up the damage until we hit the next frame.
     *
     * On the next frame, we will animate an attack from that player, apply the attack, and then
     * clear this queue so that we can receive new attacks on the network thread again.
     */
    private var queuedAttacks = mutableMapOf<Player, Int>()

    private val music: Music

    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        viewport.apply(true)
        if (!isOrthographic) {
            camera.apply {
                position.set(0f, 0f, -10f)
                lookAt(0f, 0f, 0f)
                near = 0f
                far = 1000f
                update()
            }
        }

        val menuCallback: (() -> Unit) = if (client != null) {
            { showMultiplayerMenu() }
        } else {
            { pause() }
        }

        hud = HUD(game.uiAssets, menuCallback)

        if (controller != null) {
            addGameOverlayToHUD(controller.getActor())
        }

        val strings = game.uiAssets.getStrings()
        hud.showMessage(strings[gameDetails.positiveDescription], strings[gameDetails.negativeDescription])

        music = Gdx.audio.newMusic(Gdx.files.internal(gameDetails.songAsset)).apply {
            play()
            isLooping = true
            volume = if (Options.isMute()) 0f else 1f
        }

        client?.listen(
            networkCloseListener = { code, message -> game.showNetworkError(code, message) },
            playerStatusChangedListener = { player, status -> handlePlayerStatusChange(player, status) },
            scoreChangedListener = { _, _ -> handleScoreChange() },
            scoreBreakpointListener = { player, strength -> handleBreakpointChange(player, strength) }
        )
    }

    private fun playQuietMenuMusic() {
        music.volume = 0.5f
    }

    protected fun getState() = state

    protected fun showMessage(heading: String, body: String? = null) {
        hud.showMessage(heading, body)
    }

    /**
     * Remember the position of each player (1st, 2nd, etc - taking into account equal scores),
     * so that when we get a score updated message from the server, we can tell if we have
     * changed position. If so, we will notify the user by calling [HUD.logMessage].
     */
    private var myCurrentPosition = -1

    /**
     * Calculate the order of each player, but take into account that many people can be on the
     * same score. The list is ordered such that the first element is all players who are equal 1st.
     */
    private fun calculatePlayerPositions(client: RetrowarsClient): List<Set<Player>> {
        val sortedPositions = client.scores
            .entries
            .toList()
            .sortedByDescending { it.value }

        val positions = mutableListOf<MutableSet<Player>>()

        // Temporarily store the current score so that we can build up a list of all players who have
        // this score before moving onto the next list.
        var currentScore = -1L
        var playersWithCurrentScore = mutableSetOf<Player>()

        for (p in sortedPositions) {

            if (currentScore == -1L || p.value == currentScore) {

                currentScore = p.value
                playersWithCurrentScore.add(p.key)

            } else {

                // Move onto the next position, by recording all the players at the previous score
                // and moving onto the next score.
                positions.add(playersWithCurrentScore)
                playersWithCurrentScore = mutableSetOf(p.key)
                currentScore = p.value

            }

        }

        positions.add(playersWithCurrentScore)

        return positions
    }

    private fun handleScoreChange() {
        hud.refreshScores()

        val client = this.client ?: return
        val me = client.me() ?: return

        if (isLastPlayerStanding(me, client.players)) {
            showLastSurvivorMessage()
        } else {
            val newPositions = calculatePlayerPositions(client)
            val myNewPosition = newPositions.indexOfFirst { playersAtPosition ->
                playersAtPosition.find { player ->
                    player.id == me.id
                } != null
            } + 1

            if (myNewPosition > 0 && myCurrentPosition != myNewPosition) {
                hud.logMessage(strings.format("game-message.position-changed", myNewPosition, filterActivePlayers(client.players).size))
            }

            myCurrentPosition = myNewPosition
        }
    }

    private fun handleBreakpointChange(player: Player, strength: Int) {

        if (player.id == client?.me()?.id) {
            Gdx.app.log(TAG, "Ignoring damage from player ${player.id} of strength $strength as this is the current player.")
            if (isLastPlayerStanding(client.me(), client.players)) {
                Gdx.app.log(TAG, "Also foregoing the 'attacking other players' message because there are none to attack - we are the last people remaining.")
            } else {
                hud.logMessage(strings["game-message.attacking-other-players"])
                Gdx.input.vibrate(100) // Shorter vibrate then when being attacked to differentiate.
            }
            return
        }

        Gdx.app.log(TAG, "Handling damage received from player ${player.id} of strength $strength")

        synchronized(queuedAttacks) {
            val previous = queuedAttacks[player] ?: 0
            queuedAttacks.put(player, previous + strength)
        }

    }

    private fun showLastSurvivorMessage() {
        val client = this.client ?: return
        val me = client.me() ?: return

        val highScore = client.scores.values.maxOrNull() ?: 0L
        val myScore = client.getScoreFor(me)
        hud.setPersistentMessage(strings.format("game-message.last-player-standing", highScore - myScore + 1))
    }

    private fun handlePlayerStatusChange(player: Player, status: String) {
        val client = this.client ?: return
        val me = client.me() ?: return

        if (status == Player.Status.dead) {
            if (player.id != me.id) {

                hud.handleDeadPlayer(player)

                if (isLastPlayerStanding(me, client.players)) {
                    showLastSurvivorMessage()
                } else {
                    hud.logMessage(strings["game-message.player-died"])
                }

            } else {
                Gdx.app.log(TAG, "Server has instructed us that we are in fact dead. We will honour this request and go to the end game screen.")
                endGame()
            }
        }
    }

    protected fun endGame() {
        if (state != State.Playing) {
            Gdx.app.error(TAG, "Game requested that we end, despite the fact the state is $state instead of ${State.Playing}. Sounds like the game has called endGame() more than once.")
            return
        }

        state = State.Finished

        if (isEnding) {
            return
        }

        isEnding = true

        if (client == null) {
            Gdx.app.log(RetrowarsGame.TAG, "Ending single player game... Recording high score and then loading game select menu.")
            GlobalScope.launch {
                launch { saveHighScore(gameDetails, score) }
                launch { recordStats(Stats(System.currentTimeMillis() - startTime, score, gameDetails.id)) }
            }
            showEndGameScreen()
        } else {
            Gdx.app.log(RetrowarsGame.TAG, "Ending multiplayer game... Off to the end-game lobby.")
            if (client.me()?.status == Player.Status.dead) {
                Gdx.app.debug(RetrowarsGame.TAG, "This request to head to the end game lobby was triggered by the server telling us we won, so we already know our status is dead - no need to update it.")
            } else {
                Gdx.app.debug(RetrowarsGame.TAG, "This request to head to the end game lobby was triggered by us dying. Therefore we need to notify the server that this has happened.")
                client.changeStatus(Player.Status.dead)
            }
            game.showEndMultiplayerGame()
        }
    }

    private fun showEndGameScreen() {
        val styles = game.uiAssets.getStyles()
        val skin = game.uiAssets.getSkin()
        hud.setGameOverlay(
            Table().apply {

                add(withBackground(Label(strings["game.misc.game-over"], styles.label.huge), skin)).fillX()
                row()

                val label = withBackground(Label(strings["game.misc.insert-coins"], styles.label.large), skin)

                add(label).fillX()

                label.addAction(
                    sequence(
                        alpha(0f),
                        delay(2f),
                        parallel(
                            alpha(1f, 2f),
                            Actions.run {
                                Gdx.input.inputProcessor = object: InputAdapter() {
                                    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                                        game.showMainMenu()
                                        return true
                                    }

                                    override fun keyTyped(character: Char): Boolean {
                                        game.showMainMenu()
                                        return true
                                    }
                                }
                            },
                        ),
                    )
                )
        })
    }

    protected abstract fun updateGame(delta: Float)

    /**
     * Most of the time, the [GameScreen] base class will take care of the camera management,
     * especially if it is an orthographic camera. However if you are doing something more custom
     * (e.g. see [TempestGameScreen] with its [PerspectiveCamera]), then the game itself is
     * responsible for setting up the camera, and therefore ensuring it shakes correctly in response
     * to enemy attacks in multiplayer (by applying [yOffset]).
     */
    protected open fun setupCamera(camera: Camera, yOffset: Float) {
        // Do nothing by default (i.e. for orthographic cameras).
        // If using a perspective camera, you probably want to override this and then ensure you
        // correctly apply yOffset in addition to whatever else you need to do with your camera.
    }

    protected abstract fun renderGame(camera: Camera)

    /**
     * When another player performs well, we will receive a message to tell us to get handicaped in some way.
     * e.g. for Asteroids you may add more asteroids to the screen, in Missile Command you may add more missiles.
     * However it could also be more creative, perhaps in asteroids it spins your ship around randomly, or blows you off course.
     * Perhaps in missile command it changes missiles to zig-zag down to earth, etc.
     * It is up to the game to decide how to handicap the player in response.
     *
     * The [strength] of the attack indicates how much we should handicap the current user in response.
     * The default is a strength of 1, when the other player increments their score by a certain threshold.
     * If they do something which makes their score increment by 2 or 3 times this increment, then [strength] will be 2 or 3 respectively.
     *
     * NOTE: We may receive damage from many players in a single frame. Given damage is received via network calls,
     *       we queue them up between frames to be applied at the start of the next frame. This function will be
     *       called at the start of a frame before calling [updateGame] if there is any damage to process.
     *       It will always be called in the main game thread.j
     */
    protected abstract fun onReceiveDamage(strength: Int)

    private fun maybeReceiveDamage() {
        val attacksToApply: Map<Player, Int>?
        synchronized(queuedAttacks) {
            attacksToApply = if (queuedAttacks.isNotEmpty()) {
                // We could actually call onReceiveDamage() directly here, but it could potentially
                // take a non-trivial amount of time. For example, a hypothetical game may want to
                // perform a bunch of analysis before deciding how best to apply the damage.
                // Thus, we store the value locally to be applied after we un-synchronize on the
                // main queuedDamage variable so that it can be mutated by the network thread at the
                // earliest opportunity if required.
                val copy = queuedAttacks.toMap()
                queuedAttacks.clear()
                copy
            } else {
                null
            }
        }

        if (attacksToApply != null) {
            onReceiveDamage(attacksToApply.values.sum())

            attacksToApply.onEach {
                hud.showAttackFrom(it.key, it.value)
                hud.logMessage(strings["game-message.incoming-attack"])
                startCameraShake()
            }
        }
    }

    private val shakeAnimation = ShakeAnimation(0.5f, 5, 5f)

    private fun startCameraShake() {
        shakeAnimation.shake()
        Gdx.input.vibrate(300)
    }

    private fun shakeCamera(delta: Float) {
        val shift = shakeAnimation.update(delta)
        if (shift != 0f) {
            camera.translate(0f, shift, 0f)
            camera.update()
        }
    }

    override fun render(delta: Float) {

        maybeReceiveDamage()
        shakeCamera(delta)

        if (!isPaused) {
            updateGame(delta)

            maybeSimulateAttack()
        }

        game.uiAssets.getEffects().render {
            viewport.renderIn {
                setupCamera(camera, shakeAnimation.getY())
                renderGame(camera)
            }

            hud.render(score, delta)
        }

    }

    fun getInputProcessor() = hud.getInputProcessor()

    /**
     * Provided an actor here, it will be added to the HUD over the section of the screen
     * responsible for showing the game contents.
     */
    protected fun addGameOverlayToHUD(overlay: Actor) {
        hud.setGameOverlay(overlay)
    }

    protected fun addGameScoreToHUD(score: Actor) {
        hud.addGameScore(score)
    }

    /**
     * Increments the score, and in a multiplayer game it will also notify the network client
     * to update the server.
     */
    protected fun increaseScore(scoreToIncrement: Int) {
        // We let the game continue to update after showing the Game Over screen. Therefore, there
        // is a chance that things will happen which cause scores (e.g. missiles exploding in missile command).
        // We want to make sure that these don't impact your score at all.
        if (state != State.Playing) {
            return
        }

        score += scoreToIncrement

        client?.updateScore(score)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        hud.resize(width, height)
        game.uiAssets.getEffects().resize(width, height)

        resizeViewport(viewport.worldWidth, viewport.worldHeight)
    }

    open fun resizeViewport(viewportWidth: Float, viewportHeight: Float) {
    }

    private var isPaused = false

    private fun showMultiplayerMenu() {

        val pauseGameInfo = MultiplayerInGameMenuActor(
            game.uiAssets,
            { hideInGameMenu() },
            {
                client?.listen({ _, _ -> })
                RetrowarsClient.disconnect()
                game.showMultiplayerLobby()
            },
            { isMute -> if (isMute) music.volume = 0f else playQuietMenuMusic() },
        )

        showInGameMenu(pauseGameInfo)

    }

    private fun showInGameMenu(actor: Actor) {
        val scrollView = ScrollPane(actor)
        scrollView.setFillParent(true)
        scrollView.setScrollingDisabled(true, false)
        hud.pushGameOverlay(scrollView)

        if (!Options.isMute()) {
            playQuietMenuMusic()
        }
    }

    private fun hideInGameMenu() {
        hud.popGameOverlay()
        if (!Options.isMute()) {
            music.volume = 1f
        }
    }

    final override fun pause() {
        client.also { client ->
            if (client != null) {
                game.showNetworkError(Network.ErrorCodes.CLIENT_CLOSED_APP, "Game must remain active while connected to the server.\nPlease rejoin to continue playing.")
                client.listen({ _, _ -> })
                RetrowarsClient.disconnect()
            } else {

                isPaused = true

                val pauseGameInfo = SingleplayerInGameMenuActor(
                    game.uiAssets,
                    { resume() },
                    { game.launchGame(gameDetails) },
                    { game.showGameSelectMenu() },
                    { game.showMainMenu() },
                    { isMute -> if (isMute) music.volume = 0f else playQuietMenuMusic() },
                )

                showInGameMenu(pauseGameInfo)

            }
        }
    }

    override fun resume() {
        isPaused = false
        hideInGameMenu()
    }

    override fun hide() {
        music.pause()
    }

    override fun dispose() {
        music.stop()
        music.dispose()
    }

    /**
     * Helper method for debugging, allowing players to simulate attacks by shaking the phone.
     * Useful for testing multiplayer scenarios when adding new games or working on old games.
     * Only if the []ALLOW_SIMULATED_ATTACKS] const is true.
     */
    private fun maybeSimulateAttack() {
        if (!ALLOW_SIMULATED_ATTACK) {
            return
        }

        if (!Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
            return
        }

        if (System.currentTimeMillis() < nextSimulatedAttackWindow) {
            return
        }

        if (Gdx.input.accelerometerZ < 15) {
            return
        }

        nextSimulatedAttackWindow = System.currentTimeMillis() + 1000
        hud.logMessage(strings["game-message.incoming-attack"])
        startCameraShake()
        onReceiveDamage(1)
    }

    /**
     * See [maybeSimulateAttack].
     */
    private var nextSimulatedAttackWindow = 0L
}
