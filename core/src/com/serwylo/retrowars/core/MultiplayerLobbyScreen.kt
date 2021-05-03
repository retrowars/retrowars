package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
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
                Gdx.app.postRunnable {
                    game.startGame(AsteroidsGameScreen(game, this))
                }
            }
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

            val playersWrapper = HorizontalGroup().apply {
                space(UI_SPACE)
                pad(UI_SPACE)
                renderAvatars().forEach { addActor(it) }
            }

            row()
            add(playersWrapper)

            client?.playersChangedListener = {
                Gdx.app.log(TAG, "Updating list of clients to show.")
                playersWrapper.clear()
                renderAvatars().forEach { playersWrapper.addActor(it) }
            }
        }
    }

    private fun renderAvatars(): List<WidgetGroup> {
        return client?.players?.mapIndexed { i, player ->
            AvatarTile(player, game.uiAssets, i == 0)
        } ?: emptyList()
    }

    private fun showServerLobby() {
        wrapper.apply {
            clear()

            row()
            add(Label("Server started. Waiting for others to join...", styles.label.large))

            val playersWrapper = HorizontalGroup().apply {
                space(UI_SPACE)
                pad(UI_SPACE)
                renderAvatars().forEach { addActor(it) }
            }

            row()
            add(playersWrapper).expandY()

            client?.playersChangedListener = {
                Gdx.app.log(TAG, "Updating list of clients to show.")
                playersWrapper.clear()
                renderAvatars().forEach { playersWrapper.addActor(it) }
                wrapper.invalidate()
            }

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
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

}
