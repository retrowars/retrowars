package com.serwylo.retrowars.ui

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeLargeButton
import com.serwylo.retrowars.UiAssets

class PauseGameActor(
    assets: UiAssets,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onChangeGame: () -> Unit,
    onMainMenu: () -> Unit
) : VerticalGroup() {

    private val styles = assets.getStyles()
    private val strings = assets.getStrings()

    init {

        align(Align.center)
        columnAlign(Align.center)
        space(UI_SPACE)

        val resumeButton = makeLargeButton(strings["btn.resume"], styles) { onResume() }
        val restartButton = makeButton(strings["btn.restart"], styles) { onRestart() }
        val changeGameButton = makeButton(strings["btn.change-game"], styles) { onChangeGame() }
        val mainMenuButton = makeButton(strings["btn.main-menu"], styles) { onMainMenu() }

        addActor(Label(strings["paused"], styles.label.huge))
        addActor(resumeButton)
        addActor(HorizontalGroup().apply {
            addActor(restartButton)
            addActor(changeGameButton)
            addActor(mainMenuButton)
        })
    }

}
