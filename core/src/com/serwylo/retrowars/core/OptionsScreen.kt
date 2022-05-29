package com.serwylo.retrowars.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
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

    private val styles = game.uiAssets.getStyles()
    private val skin = game.uiAssets.getSkin()
    private val strings = game.uiAssets.getStrings()
    private val sprites = game.uiAssets.getSprites()

    init {
        val stage = this.stage

        val container = Table().apply {

            setFillParent(true)
            pad(UI_SPACE)

            row().top()

            add(
                makeHeading(strings["options.title"], styles, strings) {
                    game.showMainMenu()
                }
            ).colspan(2).expandY()

            row().pad(UI_SPACE)

            add(
                Table().also { wrapper ->

                    wrapper.row().pad(UI_SPACE)
                    wrapper.add(Label(strings["options.controller-layouts"], styles.label.medium))
                    wrapper.row().pad(UI_SPACE)
                    wrapper.add(makeControllerSelectButtons())
                    wrapper.row().pad(UI_SPACE)
                    wrapper.add(makeChooseAvatarButton())

                }
            ).width(stage.viewport.worldWidth / 2f).pad(UI_SPACE).top()

            add(
                Table().also { wrapper ->
                    wrapper.add(makeVisualEffectsToggle()).growX()
                }
            ).width(stage.viewport.worldWidth / 2f).pad(UI_SPACE).top()

            row()
            add().expandY() // Used to centre the content above (as the heading also expandY()'s the top space)

        }

        stage.addActor(container)

        stage.isDebugAll = true

        addToggleAudioButtonToMenuStage(game, stage)

    }

    private fun makeControllerSelectButtons() = VerticalGroup().also { col ->
        Games.allAvailable
                .filter { it.controllerLayout != null }
                .chunked(3)
                .forEach { games ->
                    val row = HorizontalGroup()
                    col.addActor(row)
                    games.forEach { gameDetails ->
                        row.addActor(
                                IconButton(skin, gameDetails.icon(sprites)) {
                                    Gdx.app.postRunnable {
                                        game.screen = ControllerSelectScreen(game, gameDetails)
                                    }
                                }
                        )
                    }
                }
    }

    private fun makeChooseAvatarButton(): Actor {

        val action = {
            Gdx.app.postRunnable {
                game.screen = AvatarSelectScreen(game)
            }
        }

        val playerId = Options.getPlayerId()
        return if (playerId == 0L) {
            makeButton("Choose Avatar", styles, action)
        } else {
            HorizontalGroup().also { row ->
                row.addActor(Avatar(playerId, game.uiAssets))
                row.addActor(makeButton("Change Avatar", styles, action))
            }
        }
    }

    /**
     * This consists of a hack where I make my own checkbox (label + checkable box thing) because too much time was
     * spent trying to get the text in the actual [CheckBox] to wrap properly.
     */
    private fun makeVisualEffectsToggle() = Table().also { wrapper ->

        val checkbox = CheckBox("", skin).also { checkbox ->
            checkbox.isChecked = Options.useVisualEffects()
            checkbox.addListener( object: ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    val useEffects = !Options.useVisualEffects()
                    Options.setUseVisualEffects(useEffects)
                }
            })
        }

        wrapper.add(checkbox).top()

        wrapper.add(Label(strings["options.visual-effects"], styles.label.medium).also { label ->
            label.touchable = Touchable.enabled
            label.wrap = true
            label.addListener( object: ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    val useEffects = !Options.useVisualEffects()
                    checkbox.isChecked = useEffects
                    Options.setUseVisualEffects(useEffects)
                }
            })
        }).growX()

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

        val styles = game.uiAssets.getStyles()
        val strings = game.uiAssets.getStrings()
        val sprites = game.uiAssets.getSprites()

        val container = Table().apply {
            setFillParent(true)
            pad(UI_SPACE)
        }

        container.add(
            makeHeading(gameDetails.icon(sprites), strings["options.controller-select.title"], styles, strings,) {
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
        val strings = game.uiAssets.getStrings()
        heading.setText(strings.format("options.controller-select.layout-number", index + 1))
        currentIndex = index
        Options.setSoftController(gameDetails, index)
        wrapper.clear()

        val softController = SoftController(game.uiAssets, controller, index)
        if (softController.noButtonsDescription == null) {
            wrapper.add(softController.getActor()).expand().fill()
        } else {
            wrapper.add(
                Label(strings[softController.noButtonsDescription], game.uiAssets.getStyles().label.medium).apply {
                    wrap = true
                    setAlignment(Align.center)
                }
            ).expand().fill().center().pad(UI_SPACE * 2)
        }
    }

}