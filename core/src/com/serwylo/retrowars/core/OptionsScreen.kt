package com.serwylo.retrowars.core

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.scoring.Options

class OptionsScreen(game: RetrowarsGame): Scene2dScreen(game, { game.showGameSelectMenu() }) {

    init {
        val stage = this.stage
        val styles = game.uiAssets.getStyles()
        val skin = game.uiAssets.getSkin()
        val strings = game.uiAssets.getStrings()

        val container = VerticalGroup().apply {
            setFillParent(true)
            align(Align.center)
            space(UI_SPACE)
        }

        container.addActor(
            makeHeading(strings["options.title"], styles, strings) {
                game.showMainMenu()
            }
        )

        container.addActor(
            CheckBox(strings["options.visual-effects"], skin).apply {

                isChecked = Options.useVisualEffects()

                addListener( object: ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        Options.setUseVisualEffects(!Options.useVisualEffects())
                    }
                })

            }
        )

        stage.addActor(container)

    }

}
