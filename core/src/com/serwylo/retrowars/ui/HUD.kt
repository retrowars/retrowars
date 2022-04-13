package com.serwylo.retrowars.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient
import java.util.*

class HUD(private val assets: UiAssets, onMenu: (() -> Unit)?) {

    companion object {
        private const val TAG = "HUD"
    }

    /**
     * The bottom slither of the stage dedicated to showing stats, multiplayer info, etc.
     * The game screen will not fill into here. There will be a small amount of space for
     * game-specific info (e.g. lives in Asteroids), but for the most part it will al lbe
     * generic across games: Score, multiplayer info, etc.
     */
    private val infoCell: Cell<Table>
    private val avatarCell: Cell<WidgetGroup>?
    private val avatars: Map<Player, Avatar>
    private val styles = assets.getStyles()
    private val strings = assets.getStrings()

    private val stage = makeStage()
    private val scoreLabel = Label("", styles.label.large)
    private var gameOverlay = Container<Actor>().apply { fill() }
    private var descriptionHeading = Label(null, styles.label.large).apply { setAlignment(Align.center) }
    private var descriptionBody  = Label(null, styles.label.medium).apply { setAlignment(Align.center) }
    private var description = VerticalGroup().apply {
        addActor(
            withBackground(
                Table().apply {
                    row().top().spaceTop(UI_SPACE * 10)
                    add(descriptionHeading)
                    row()
                    add(descriptionBody)
                },
                assets.getSkin(),
            )
        )
        
        align(Align.top)
        padTop(UI_SPACE * 10)
    }

    /**
     * A rolling log of game events shown to the user in the top right of the screen.
     * Messages are transient, start large, then go small and then fade away.
     *
     * Call [logMessage] to post new messages to the top of the list.
     */
    private var messages = VerticalGroup().apply {
        setFillParent(true)
        reverse()
        pad(UI_SPACE)
        space(UI_SPACE)
        columnAlign(Align.right)
        align(Align.topRight)
    }

    private val gameScore: Cell<Actor>

    private val client = RetrowarsClient.get()

    init {

        // The "game window" is pure decoration. The actual viewport which aligns with this same
        // amount of space is managed by the game via the [GameViewport].
        val gameWindow = Table()
        gameWindow.background = assets.getSkin().getDrawable("window-empty")

        gameWindow.add(
            Stack(
                description,
                messages,
                gameOverlay,
            )
        ).expand().fill()

        avatars = client?.players?.associateWith { Avatar(it.id, assets) } ?: emptyMap()

        val infoWindow = Table().apply {
            background = assets.getSkin().getDrawable("window-empty")
            pad(UI_SPACE)

            row()

            add(scoreLabel).pad(UI_SPACE)

            // Hold a reference to this so that we can update (naively blow away and recreate) the
            // multiplayer info when the client updates us, without having to throw away too many other
            // parts of the UI.
            avatarCell = if (client == null) null else add(makeAvatarTiles(client, avatars)).expandX().left().pad(UI_SPACE)

            gameScore = add().right().expandX().pad(UI_SPACE)

            if (onMenu != null) {
                add(
                    makeSmallButton(strings["btn.menu"], styles, onMenu)
                )
            }
        }

        val windowManager = Table().apply {
            setFillParent(true)

            add(gameWindow).expand().fill()

            row()

            infoCell = add(infoWindow).expandX().fillX()
        }

        stage.addActor(windowManager)

    }

    fun getInputProcessor(): InputProcessor = stage

    fun render(score: Long, delta: Float) {

        scoreLabel.setText(score.toString())

        stage.act(delta)
        stage.draw()

    }

    private val overlays = LinkedList<Actor>()

    fun setGameOverlay(overlay: Actor) {
        gameOverlay.actor = overlay
        overlays.clear()
        overlays.push(overlay)
    }

    fun pushGameOverlay(overlay: Actor) {
        gameOverlay.actor = overlay
        overlays.push(overlay)
    }

    fun popGameOverlay() {
        overlays.pop()
        if (overlays.isEmpty()) {
            gameOverlay.clear()
        } else {
            gameOverlay.actor = overlays.last
        }
    }

    fun addGameScore(overlay: Actor) {
        gameScore.setActor(overlay)
    }

    private var persistentMessage: Label? = null

    /**
     * Post a permanent message in the top right of the screen. Only one message can be shown
     * at once.
     * Note: Prefer to use [logMessage], because otherwise the screen will quickly fill up with
     *       messages that never leave.
     */
    fun setPersistentMessage(message: String) {
        persistentMessage.let { existingMessage ->
            if (existingMessage == null) {
                val newMessage = Label(message, styles.label.medium).apply {
                    addAction(
                        sequence(
                            delay(5f),
                            Actions.run { style = styles.label.small },
                        )
                    )
                }
                persistentMessage = newMessage
                messages.addActor(newMessage)
            } else {
                existingMessage.setText(message)
            }
        }
    }

    /**
     * Post a transient message to the top right of the screen. Starts large, then goes smaller
     * and then eventually fades away.
     */
    fun logMessage(message: String) {
        messages.addActor(Label(message, styles.label.medium).apply {
            addAction(
                sequence(
                    delay(5f),
                    Actions.run { style = styles.label.small },
                    delay(5f),
                    alpha(0f, 0.5f),
                    removeActor(),
                )
            )
        })
    }

    /**
     * Overlay a message in the middle of the screen in large text with an optional description below.
     * Examples include a nice single line of text when first starting the game (e.g. "Defend the cities" or "Destroy the asteroids")
     */
    fun showMessage(heading: String, body: String? = null) {
        this.descriptionHeading.setText(heading)
        this.descriptionBody.setText(body)
        this.description.addAction(
            sequence(
                alpha(0f, 0f), // Start at 0f alpha (hence duration 0f)...
                alpha(1f, 0.2f), // ... and animate to 1.0f quite quickly.
                delay(2f),
                alpha(0f, 0.5f)
            )
        )
    }

    private fun calcInfoHeight(): Float {
        val infoHeight = stage.height * GameViewport.BOTTOM_OFFSET_SCREEN_PROPORTION
        val minHeight = stage.height - stage.viewport.unproject(Vector2(0f, GameViewport.BOTTOM_OFFSET_MIN_SCREEN_PX.toFloat())).y
        val maxHeight = stage.height - stage.viewport.unproject(Vector2(0f, GameViewport.BOTTOM_OFFSET_MAX_SCREEN_PX.toFloat())).y

        return infoHeight.coerceIn(minHeight, maxHeight)
    }

    fun resize(screenWidth: Int, screenHeight: Int) {
        stage.viewport.update(screenWidth, screenHeight, true)
        infoCell.height(calcInfoHeight())
    }

    private fun makeAvatarTiles(client: RetrowarsClient, avatars: Map<Player, Avatar>): WidgetGroup {
        return Table().apply {
            val me = client.me()
            val myAvatar = avatars[me]
            if (me != null) {
                if (myAvatar == null) {
                    Gdx.app.error(TAG, "Expected avatar for myself (${me.id}), but couldn't find it.")
                } else {
                    add(myAvatar)
                }
            }

            val others = client.otherPlayers()
            if (others.isNotEmpty()) {
                if (me != null) {
                    add(Label("vs", assets.getStyles().label.medium)).spaceLeft(UI_SPACE * 3).spaceRight(UI_SPACE * 3)
                }

                others.onEach { player ->
                    val avatar = avatars[player]
                    if (avatar == null) {
                        Gdx.app.error(TAG, "Expected avatar for ${player.id}, but couldn't find it.")
                    } else {
                        val label = Label(client.getScoreFor(player).toString(), styles.label.medium)
                        if (player.status == Player.Status.dead) {
                            label.color = Color(0.6f, 0.6f, 0.6f, 1f)
                        }
                        add(label)
                        add(avatar).spaceRight(UI_SPACE * 2)
                    }
                }
            }
        }
    }

    fun showAttackFrom(player: Player, strength: Int) {
        val avatar = avatars[player] ?: return
        avatar.addAction(CustomActions.bounce(strength * 3))
    }

    fun refreshScores() {
        if (client != null) {
            avatarCell?.apply {
                clearActor()
                setActor(makeAvatarTiles(client, avatars))
            }
        }
    }

    fun handleDeadPlayer(player: Player) {
        avatars[player]?.isDead = true
        refreshScores()
    }

}