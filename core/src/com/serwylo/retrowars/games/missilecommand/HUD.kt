package com.serwylo.retrowars.games.missilecommand

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeStage
import com.serwylo.retrowars.UiAssets

class HUD(private val state: MissileCommandGameState, assets: UiAssets) {

    private val styles = assets.getStyles()

    private val stage = makeStage()
    private val scoreLabel = Label("", styles.label.large)

    init {

        val table = Table()
        table.setFillParent(true)
        table.pad(UI_SPACE)
        table.row().expand()
        table.add(scoreLabel).right().bottom()

        stage.addActor(table)

    }

    fun render(delta: Float) {

        scoreLabel.setText(state.score.toString())

        stage.act(delta)
        stage.draw()

    }

}