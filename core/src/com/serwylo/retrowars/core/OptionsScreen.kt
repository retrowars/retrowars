package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.AsteroidsSoftController
import com.serwylo.retrowars.input.SnakeSoftController
import com.serwylo.retrowars.input.SoftController
import com.serwylo.retrowars.input.TetrisSoftController
import com.serwylo.retrowars.ui.IconButton
import com.serwylo.retrowars.utils.Options

class OptionsScreen(game: RetrowarsGame): Scene2dScreen(game, { game.showMainMenu() }) {

    init {
        val stage = this.stage
        val styles = game.uiAssets.getStyles()
        val skin = game.uiAssets.getSkin()
        val strings = game.uiAssets.getStrings()
        val sprites = game.uiAssets.getSprites()

        val container = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)
        }

        container.row().top()
        container.add(
            makeHeading(strings["options.title"], styles, strings) {
                game.showMainMenu()
            }
        ).expandY()

        container.row().pad(UI_SPACE)

        container.add(
            CheckBox(strings["options.visual-effects"], skin).apply {

                isChecked = Options.useVisualEffects()

                addListener( object: ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        Options.setUseVisualEffects(!Options.useVisualEffects())
                    }
                })

            }
        )

        container.row().pad(UI_SPACE)
        container.add(
            Label("Controller layouts", styles.label.medium)
        )

        container.row()
        container.add(
            HorizontalGroup().apply {

                addActor(
                    IconButton(skin, Games.asteroids.icon(sprites)) {
                        Gdx.app.postRunnable {
                            game.screen = ControllerSelectScreen(
                                game,
                                Games.asteroids,
                                AsteroidsSoftController.layouts,
                            ) { index -> AsteroidsSoftController(index, game.uiAssets) }
                        }
                    }
                )

                addActor(
                    IconButton(skin, Games.snake.icon(sprites)) {
                        Gdx.app.postRunnable {
                            game.screen = ControllerSelectScreen(
                                game,
                                Games.snake,
                                SnakeSoftController.layouts,
                            ) { index -> SnakeSoftController(index, game.uiAssets) }
                        }
                    }
                )

                addActor(
                    IconButton(skin, Games.tetris.icon(sprites)) {
                        Gdx.app.postRunnable {
                            game.screen = ControllerSelectScreen(
                                game,
                                Games.tetris,
                                TetrisSoftController.layouts,
                            ) { index -> TetrisSoftController(index, game.uiAssets) }
                        }
                    }
                )

            }
        ).expandY().top()

        stage.addActor(container)

    }

}

class ControllerSelectScreen(
    game: RetrowarsGame,
    private val gameDetails: GameDetails,
    private val keyboards: List<String>,
    private val makeController: (index: Int) -> SoftController,
): Scene2dScreen(game, { game.showOptions() }) {

    private val wrapper = Table()
    private val heading = Label("", game.uiAssets.getStyles().label.large)
    private var currentIndex = Options.getSoftController(gameDetails)

    init {

        val container = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)
        }

        container.add(
            makeHeading(
                gameDetails.icon(game.uiAssets.getSprites()),
                "Controller",
                game.uiAssets.getStyles(),
                game.uiAssets.getStrings()
            ) {
                game.showOptions()
            }
        ).top().colspan(3)

        container.row()

        container.add(heading).top().colspan(3).spaceBottom(UI_SPACE)

        container.row()

        container.add(
            makeButton("<", game.uiAssets.getStyles()) {
                setSelection((currentIndex + keyboards.size - 1) % keyboards.size)
            }
        ).fillY()

        wrapper.background = game.uiAssets.getSkin().getDrawable("window")
        wrapper.pad(UI_SPACE)
        container.add(wrapper).expand().fill().padLeft(UI_SPACE).padRight(UI_SPACE)

        container.add(
            makeButton(">", game.uiAssets.getStyles()) {
                setSelection((currentIndex + 1) % keyboards.size)
            }
        ).fillY()

        setSelection(currentIndex)

        stage.addActor(container)
    }

    private fun setSelection(index: Int) {
        heading.setText("Layout ${index + 1}")
        currentIndex = index
        Options.setSoftController(gameDetails, index)
        wrapper.clear()
        wrapper.add(makeController(index).getActor()).expand().fill()
    }

}