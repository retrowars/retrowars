package com.serwylo.retrowars.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.serwylo.beatgame.ui.Avatar
import com.serwylo.beatgame.ui.CustomActions
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeStage
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.net.Player
import com.serwylo.retrowars.net.RetrowarsClient

class HUD(private val assets: UiAssets) {

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

    private val stage = makeStage()
    private val scoreLabel = Label("", styles.label.large)
    private var gameOverlay: Cell<Actor>
    private val gameScore: Cell<Actor>

    private val client = RetrowarsClient.get()

    init {

        // The "game window" is pure decoration. The actual viewport which aligns with this same
        // amount of space is managed by the game via the [GameViewport].
        val gameWindow = Table()
        gameWindow.background = assets.getSkin().getDrawable("window")
        gameOverlay = gameWindow.add().expand().fill()
        avatars = client?.players?.associateWith { Avatar(it, assets) } ?: emptyMap()

        val infoWindow = Table().apply {
            background = assets.getSkin().getDrawable("window")
            pad(UI_SPACE)

            row()

            add(scoreLabel).pad(UI_SPACE)

            // Hold a reference to this so that we can update (naively blow away and recreate) the
            // multiplayer info when the client updates us, without having to throw away too many other
            // parts of the UI.
            avatarCell = if (client == null) null else add(makeAvatarTiles(client, avatars)).expandX().left().pad(UI_SPACE)

            gameScore = add().right().expandX().pad(UI_SPACE)
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

    fun addGameOverlay(overlay: Actor) {
        gameOverlay.setActor(overlay)
    }

    fun addGameScore(overlay: Actor) {
        gameScore.setActor(overlay)
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
                        add(avatar)
                    }
                }
            }
        }
    }

    fun showAttackFrom(player: Player, strength: Int) {
        val avatar = avatars[player] ?: return
        avatar.addAction(CustomActions.bounce(strength * 3))
    }

}