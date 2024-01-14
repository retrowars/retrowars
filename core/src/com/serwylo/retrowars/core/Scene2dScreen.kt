package com.serwylo.retrowars.core

import com.badlogic.gdx.*
import com.serwylo.retrowars.ui.makeStage
import com.serwylo.retrowars.RetrowarsGame

abstract class Scene2dScreen(protected val game: RetrowarsGame, private val onBack: (() -> Unit)? = null): ScreenAdapter() {

    protected val stage = makeStage()

    private val effects = game.uiAssets.getEffects()

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        effects.resize(width, height)
    }

    override fun show() {
        if (onBack == null) {
            Gdx.input.inputProcessor = stage
        } else {
            val backHandler = onBack
            Gdx.input.setCatchKey(Input.Keys.BACK, true)
            Gdx.input.inputProcessor = InputMultiplexer(stage, object : InputAdapter() {

                override fun keyDown(keycode: Int): Boolean {
                    if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                        backHandler()
                        return true
                    }

                    return false
                }

            })
        }
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
    }

    override fun render(delta: Float) {
        stage.act(delta)
        effects.render {
            stage.draw()
        }
    }

}
