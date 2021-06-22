package com.serwylo.retrowars.games

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.withBackground
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import com.serwylo.retrowars.scoring.Stats
import com.serwylo.retrowars.scoring.recordStats
import com.serwylo.retrowars.scoring.saveHighScore
import com.serwylo.retrowars.ui.GameViewport
import com.serwylo.retrowars.ui.HUD
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class GameScreen(protected val game: RetrowarsGame, private val gameDetails: GameDetails, minWorldWidth: Float, maxWorldWidth: Float) : Screen {

    companion object {
        const val TAG = "GameScreen"
    }

    enum class State {
        Playing,
        Finished,
    }

    private val camera = OrthographicCamera()
    protected val viewport = GameViewport(minWorldWidth, maxWorldWidth, camera)

    private val hud: HUD

    private val startTime = System.currentTimeMillis()

    protected val client = RetrowarsClient.get()

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

    init {
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        viewport.apply(true)
        hud = HUD(game.uiAssets)

        client?.listen(
            networkCloseListener = { wasGraceful -> game.showNetworkError(game, wasGraceful) },
            playerStatusChangedListener = { player, status -> handlePlayerStatusChange(player, status) },
            scoreBreakpointListener = { player, strength -> handleBreakpointChange(player, strength) }
        )
    }

    protected fun getState() = state

    protected fun showMessage(heading: String, body: String? = null) {
        hud.showMessage(heading, body)
    }

    private fun handleBreakpointChange(player: Player, strength: Int) {

        if (player.id == client?.me()?.id) {
            // TODO: Show visual feedback that we are attacking other players.
            Gdx.app.log(TAG, "Ignoring damage from player ${player.id} of strength $strength as this is the current player.")
            return
        }

        Gdx.app.log(TAG, "Handling damage received from player ${player.id} of strength $strength")

        synchronized(queuedAttacks) {
            val previous = queuedAttacks[player] ?: 0
            queuedAttacks.put(player, previous + strength)
        }

    }

    private fun handlePlayerStatusChange(player: Player, status: String) {
        val client = this.client ?: return

        if (player.id != client.me()?.id) {
            return
        }

        if (status == Player.Status.dead) {
            Gdx.app.log(TAG, "Server has instructed us that we are in fact dead. We will honour this request and go to the end game screen.")
            endGame()
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

        // TODO: Show end of game screen.
        if (client == null) {
            Gdx.app.log(RetrowarsGame.TAG, "Ending single player game... Recording high score and then loading game select menu.")
            GlobalScope.launch {
                saveHighScore(gameDetails, score)
                recordStats(Stats(System.currentTimeMillis() - startTime, score, gameDetails.id))
            }
            showEndGameScreen()
        } else {
            Gdx.app.log(RetrowarsGame.TAG, "Ending multiplayer game... Off to the end-game lobby.")
            client.changeStatus(Player.Status.dead)
            game.showEndMultiplayerGame()
        }
    }

    private fun showEndGameScreen() {
        val styles = game.uiAssets.getStyles()
        val skin = game.uiAssets.getSkin()
        hud.addGameOverlay(
            Table().apply {

                add(withBackground(Label("Game Over", styles.label.huge), skin)).fillX()
                row()

                val label = withBackground(Label("Insert coin\nto continue", styles.label.large), skin)

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
    protected abstract fun renderGame(camera: OrthographicCamera)

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
            }
        }
    }

    override fun render(delta: Float) {

        maybeReceiveDamage()
        updateGame(delta)

        game.uiAssets.getEffects().render {
            viewport.renderIn {
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
        hud.addGameOverlay(overlay)
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

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
    }
}
