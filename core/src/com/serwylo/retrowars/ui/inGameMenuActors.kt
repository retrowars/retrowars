package com.serwylo.retrowars.ui

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.serwylo.retrowars.UiAssets

class SingleplayerInGameMenuActor(
    assets: UiAssets,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onChangeGame: () -> Unit,
    onMainMenu: () -> Unit,
    onToggleAudio: (volume: Float) -> Unit,
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
        val toggleAudioButton = makeToggleAudioButtons(assets.getSprites(), onToggleAudio, { })

        addActor(Label(strings["in-game-menu.paused-menu-title"], styles.label.huge))
        addActor(resumeButton)
        addActor(HorizontalGroup().apply {
            addActor(toggleAudioButton)
            addActor(restartButton)
            addActor(changeGameButton)
            addActor(mainMenuButton)
        })

    }

}

class MultiplayerInGameMenuActor(
    assets: UiAssets,
    onCloseMenu: () -> Unit,
    onLeaveGame: () -> Unit,
    onToggleAudio: (volume: Float) -> Unit,
) : VerticalGroup() {

    private val styles = assets.getStyles()
    private val strings = assets.getStrings()

    init {

        align(Align.center)
        columnAlign(Align.center)
        space(UI_SPACE)

        val resumeButton = makeLargeButton(strings["btn.hide-menu"], styles) { onCloseMenu() }
        val leaveGameButton = makeButton(strings["btn.leave-game"], styles) { onLeaveGame() }
        val toggleAudioButton = makeToggleAudioButtons(assets.getSprites(), onToggleAudio, { })

        addActor(Label(strings["in-game-menu.multiplayer-menu-title"], styles.label.huge))
        addActor(Label(strings["in-game-menu.multiplayer-cannot-be-paused"], styles.label.small))
        addActor(resumeButton)
        addActor(HorizontalGroup().apply {
            addActor(toggleAudioButton)
            addActor(leaveGameButton)
        })
    }

}
