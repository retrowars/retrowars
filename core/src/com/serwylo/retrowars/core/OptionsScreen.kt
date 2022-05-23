package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.serwylo.beatgame.ui.*
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.beatgame.ui.makeButton
import com.serwylo.beatgame.ui.makeHeading
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.SoftController
import com.serwylo.retrowars.ui.IconButton
import com.serwylo.retrowars.utils.Options
import kotlin.random.Random

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

            row().top()
            add(
                makeHeading(strings["options.title"], styles, strings) {
                    game.showMainMenu()
                }
            ).expandY()

            row().pad(UI_SPACE)
            add(
                CheckBox(strings["options.visual-effects"], skin).apply {

                    isChecked = Options.useVisualEffects()

                    addListener( object: ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            Options.setUseVisualEffects(!Options.useVisualEffects())
                        }
                    })

                }
            )

            row().padTop(UI_SPACE * 2).padBottom(UI_SPACE)
            add(
                Label(strings["options.controller-layouts"], styles.label.medium)
            )

            row()
            add(
                HorizontalGroup().apply {
                    Games.allAvailable
                        .filter { it.controllerLayout != null }
                        .forEach { gameDetails ->
                            addActor(
                                IconButton(skin, gameDetails.icon(sprites)) {
                                    Gdx.app.postRunnable {
                                        game.screen = ControllerSelectScreen(game, gameDetails)
                                    }
                                }
                            )


                    }
                }
            )

            row().padTop(UI_SPACE * 2).padBottom(UI_SPACE)
            add(
                Label(strings["options.multiplayer-avatar"], styles.label.medium)
            )

            row()
            val playerId = Options.getPlayerId()
            val avatar = if (playerId == 0L) {
                Label("?", game.uiAssets.getStyles().label.medium)
            } else {
                Avatar(playerId, game.uiAssets)
                }

            add(
                Button(avatar, game.uiAssets.getSkin()).apply {
                    addListener(object: ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            Gdx.app.postRunnable {
                                game.screen = AvatarSelectScreen(game)
                            }
                        }
                    })
                }
            ).expandY().top()

        }

        stage.addActor(container)

        addToggleAudioButtonToMenuStage(game, stage)

    }

}

class AvatarSelectScreen(
    game: RetrowarsGame,
): Scene2dScreen(game, { game.showOptions() }) {

    private var currentPlayerId = Options.getPlayerId()

    private var strings = game.uiAssets.getStrings()

    init {

        val container = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)

            add(
                makeHeading(
                    strings["options.avatar.choose"],
                    game.uiAssets.getStyles(),
                    game.uiAssets.getStrings()
                ) {
                    game.showOptions()
                }
            ).expandY().top()

            if (currentPlayerId == 0L) {

                row()
                add(
                    Label(
                        strings["options.avatar.choose-description-empty"],
                        game.uiAssets.getStyles().label.medium
                    ).apply {
                        setAlignment(Align.center)
                    }
                )

            } else {

                row()
                add(Label(strings["options.avatar.current"], game.uiAssets.getStyles().label.medium))

                row()
                add(
                    HorizontalGroup().apply {

                        addActor(Avatar(currentPlayerId, game.uiAssets))

                        addActor(makeSmallButton(strings["options.avatar.btn-clear"], game.uiAssets.getStyles()) {
                            Options.setPlayerId(0)
                            Gdx.app.postRunnable {
                                game.screen = AvatarSelectScreen(game)
                            }
                        })

                        addActor(makeSmallButton(strings["options.avatar.btn-help"], game.uiAssets.getStyles()) {
                            Gdx.net.openURI("https://github.com/retrowars/retrowars/wiki/Avatars")
                        })

                    }
                )

                row()
                add(Label(strings["options.avatar.choose-description"], game.uiAssets.getStyles().label.medium))

            }

            row()
            val avatarGridCell: Cell<Table> = add(makeAvatarGrid(game.uiAssets))

            row()
            add(
                makeButton(strings["options.avatar.btn-show-more"], game.uiAssets.getStyles()) {
                    avatarGridCell.setActor(makeAvatarGrid(game.uiAssets))
                }
            ).expandY().top()

        }

        stage.addActor(container)
    }

    private fun makeAvatarGrid(uiAssets: UiAssets): Table {
        return Table().apply {
            for (y in 0 until 2) {
                row()
                for (x in 0 until 6) {
                    val id = Random.nextInt()
                    add(
                        Button(
                            Avatar(id.toLong(), uiAssets),
                            uiAssets.getSkin(),
                        ).apply {
                            addListener(object: ChangeListener() {
                                override fun changed(event: ChangeEvent?, actor: Actor?) {
                                    Gdx.app.postRunnable {
                                        Options.setPlayerId(id.toLong())
                                        game.screen = OptionsScreen(game)
                                    }
                                }
                            })
                        }
                    ).pad(UI_SPACE)
                }
            }
        }
    }
}

class ControllerSelectScreen(
    game: RetrowarsGame,
    private val gameDetails: GameDetails,
): Scene2dScreen(game, { game.showOptions() }) {

    private val wrapper = Table()
    private val heading = Label("", game.uiAssets.getStyles().label.large)
    private val controller = gameDetails.controllerLayout!!
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
                setSelection((currentIndex + controller.getLayouts().size - 1) % controller.getLayouts().size)
            }
        ).fillY()

        wrapper.background = game.uiAssets.getSkin().getDrawable("window")
        wrapper.pad(UI_SPACE)
        container.add(wrapper).expand().fill().padLeft(UI_SPACE).padRight(UI_SPACE)

        container.add(
            makeButton(">", game.uiAssets.getStyles()) {
                setSelection((currentIndex + 1) % controller.getLayouts().size)
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

        val softController = SoftController(game.uiAssets, controller, index)
        if (softController.noButtonsDescription == null) {
            wrapper.add(softController.getActor()).expand().fill()
        } else {
            wrapper.add(
                Label(softController.noButtonsDescription, game.uiAssets.getStyles().label.medium).apply {
                    wrap = true
                    setAlignment(Align.center)
                }
            ).expand().fill().center().pad(UI_SPACE * 2)
        }
    }

}