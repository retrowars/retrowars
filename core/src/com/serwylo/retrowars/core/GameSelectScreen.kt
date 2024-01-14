package com.serwylo.retrowars.core

import com.badlogic.gdx.*
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.serwylo.retrowars.ui.UI_SPACE
import com.serwylo.retrowars.ui.makeHeading
import com.serwylo.retrowars.ui.makeStage
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.scoring.loadHighScore

class GameSelectScreen(private val game: RetrowarsGame): ScreenAdapter() {

    private val stage = makeStage()

    private val styles = game.uiAssets.getStyles()
    private val effects = game.uiAssets.getEffects()
    private val skin = game.uiAssets.getSkin()
    private val strings = game.uiAssets.getStrings()
    private val sprites = game.uiAssets.getSprites()

    init {
        setupStage()
    }

    private fun setupStage() {
        // Later on, do some proper responsive sizing. However my first attempts struggled with
        // density independent pixel calculations (even though the math is simple, it didn't
        // seem to set proper breakpoints, perhaps because of the arbitrary math in calcDensityScaleFactor()
        // from before it occurred we could use DIPs).
        val gamesPerRow = 5
        val width = (stage.width - UI_SPACE * 4) / gamesPerRow
        val height = width * 6 / 5

        val container = VerticalGroup().apply {
            space(UI_SPACE)
            padTop(UI_SPACE * 2)
        }

        val scrollPane = ScrollPane(container, skin).apply {
            setFillParent(true)
            setScrollingDisabled(true, false)
            setupOverscroll(width / 4, 30f, 200f)
        }

        stage.addActor(scrollPane)

        container.addActor(
            makeHeading(strings["game-select.title"], styles, strings) {
                game.showMainMenu()
            }
        )

        val table = Table().apply {
            pad(UI_SPACE)
        }

        container.addActor(table)

        var x = 0
        var y = 0

        val games = Games.allReleased + Games.allBeta + Games.other
        games.forEachIndexed { i, game ->

            if (i % gamesPerRow == 0) {
                table.row()
                y ++
                x = 0
            }

            table.add(makeButton(game)).width(width).height(height)

            x ++

        }

    }

    override fun show() {

        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = InputMultiplexer(stage, object : InputAdapter() {

            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    game.showMainMenu()
                    return true
                }

                return false
            }

        })

    }

    override fun hide() {
        Gdx.input.inputProcessor = null
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        stage.clear()
        setupStage()
        effects.resize(width, height)
    }

    override fun render(delta: Float) {
        stage.act(delta)
        effects.render {
            stage.draw()
        }
    }

    override fun dispose() {
        stage.dispose()
    }

    private fun makeButton(game: GameDetails): WidgetGroup {

        val isLocked = false
        val buttonStyle = if (isLocked) "locked" else "default"

        val button = Button(skin, buttonStyle).apply {
            isDisabled = isLocked
            setFillParent(true)
            addListener(object: ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    onGameSelected(game)
                }
            })
        }

        val table = Table().apply {
            setFillParent(true)
            touchable = Touchable.disabled // Let the button in the background do the interactivity.
            pad(UI_SPACE * 2)
        }

        val icon = Image(game.icon(sprites)).apply {
            setScaling(Scaling.fit)
            align = Align.top
        }

        table.row()
        table.add(icon).expand().bottom().fill().pad(UI_SPACE * 2)

        val highScore = loadHighScore(game)
        val subText = if (!game.isAvailable) {
            strings["unimplemented-game.coming-soon"]
        } else if (highScore.attempts > 0) {
            "${strings["game-select.high-score"]}\n${highScore.score}"
        } else {
            ""
        }

        table.row()
        table.add(
            Label(subText, styles.label.small).apply {
                setAlignment(Align.center)
            }
        ).expandX()

        return Stack().also { stack ->
            stack.addActor(button)
            stack.addActor(table)
            if (Games.allBeta.contains(game)) {
                stack.addActor(
                    Table().also { betaTable ->
                        betaTable.setFillParent(true)
                        betaTable.pad(UI_SPACE * 1.5f)
                        betaTable.add(Label("Beta", styles.label.small)).top().expandY()
                    }
                )
            }
        }

    }

    fun onGameSelected(gameDetails: GameDetails) {
        val betaInfo = Games.betaInfo.find { it.game === gameDetails }
        if (betaInfo == null) {
            this.game.launchGame(gameDetails)
        } else {
            this.game.showBetaDetails(gameDetails, betaInfo)
        }
    }

}

