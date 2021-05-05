package com.serwylo.retrowars.games.asteroids

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeAvatarTiles
import com.serwylo.beatgame.ui.makeStage
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.net.RetrowarsClient

class HUD(private val state: AsteroidsGameState, assets: UiAssets) {

    private val styles = assets.getStyles()

    private val stage = makeStage()
    private val lifeContainer = HorizontalGroup()
    private val scoreLabel = Label("", styles.label.large)

    private val client = RetrowarsClient.get()

    init {

        val table = Table()
        table.setFillParent(true)
        table.pad(UI_SPACE)
        table.row().expand()
        table.add(lifeContainer).left().bottom()

        val networkPlayers = client?.players
        if (networkPlayers != null) {
            table.add(makeAvatarTiles(networkPlayers, assets)).center().bottom()
        }
        table.add(scoreLabel).right().bottom()

        lifeContainer.space(UI_SPACE * 2)

        stage.addActor(table)

    }

    fun render(delta: Float) {

        scoreLabel.setText(state.score.toString())

        if (lifeContainer.children.size != state.numLives) {
            redrawLives()
        }

        stage.act(delta)
        stage.draw()
    }

    private fun redrawLives() {
        lifeContainer.clear()
        for (i in 0 until state.numLives) {
            lifeContainer.addActor(Label("x", styles.label.large))
        }
    }

}