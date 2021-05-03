package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.serwylo.beatgame.ui.*
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.asteroids.AsteroidsGameScreen
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

    private var server: RetrowarsServer? = null
    private var client: RetrowarsClient? = null

    init {
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

        server = RetrowarsServer()
        createClient()

        Gdx.app.log(TAG, "Server started.")

        showServerLobby()

    }

    private fun createClient() {
        client = RetrowarsClient().apply {
            connect()
            startGameListener = {
                initiateStartCountdown()
            }
        }
    }

    private fun initiateStartCountdown() {
        wrapper.apply {
            clear()

            var counter = 5
            val countdown = Label(counter.toString(), game.uiAssets.getStyles().label.huge)
            val countdownContainer = Container(countdown).apply { isTransform = true }

            row()
            add(countdownContainer).center().expand()

            countdownContainer.addAction(
                sequence(
                    repeat(counter,
                        parallel(
                            Actions.run {
                                counter --
                                countdown.setText((counter + 1).toString())
                            },
                            sequence(
                                alpha(0f, 0f),
                                alpha(1f, 0.4f)
                            ),
                            sequence(
                                scaleTo(3f, 3f, 0f),
                                scaleTo(1f, 1f, 0.75f)
                            ),
                            delay(1f)
                        )
                    ),
                    Actions.run {
                        Gdx.app.postRunnable {
                            game.startGame(AsteroidsGameScreen(game, client))
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

        client?.playersChangedListener = { players ->
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
                server?.startGame()
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
        server?.close()
        client?.close()
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
