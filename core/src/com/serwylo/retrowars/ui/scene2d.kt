package com.serwylo.beatgame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.games.GameDetails
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.net.Player
import kotlin.random.Random

fun makeStage() =
    Stage(ExtendViewport(UI_WIDTH, UI_HEIGHT))

fun makeButton(label: String, styles: UiAssets.Styles, onClick: () -> Unit): Button {
    return TextButton(label, styles.textButton.medium).apply {
        addListener(object: ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                onClick()
            }
        })
    }
}

fun makeLargeButton(label: String, styles: UiAssets.Styles, onClick: () -> Unit): Button {
    return TextButton(label, styles.textButton.large).apply {
        addListener(object: ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                onClick()
            }
        })
    }
}

fun makeSmallButton(label: String, styles: UiAssets.Styles, onClick: () -> Unit): Button {
    return TextButton(label, styles.textButton.small).apply {
        addListener(object: ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                onClick()
            }
        })
    }
}

fun makeIcon(sprite: TextureRegion, size: Float = UI_SPACE * 4): Image {
    return Image(
        makeIconDrawable(sprite, size)
    )
}

fun makeIconDrawable(sprite: TextureRegion, size: Float = UI_SPACE * 4): TextureRegionDrawable {
    return TextureRegionDrawable(sprite).apply {
        setMinSize(size, size)
    }
}

fun makeHeading(title: String, styles: UiAssets.Styles, strings: I18NBundle, onBack: (() -> Unit)? = null): HorizontalGroup {
    return HorizontalGroup().apply {
        space(UI_SPACE * 2)
        padBottom(UI_SPACE)
        addActor(Label(title, styles.label.huge))
        if (onBack != null) {
            addActor(makeSmallButton(strings["btn.back"], styles, onBack))
        }
    }
}

val UI_WIDTH = 1024f / calcDensityScaleFactor()
val UI_HEIGHT = 768f / calcDensityScaleFactor()
const val UI_SPACE = 10f

fun calcDensityScaleFactor(): Float {
    return ((Gdx.graphics.density - 1) * 0.8f).coerceAtLeast(1f)
}

// TODO: When players are no longer playing, show a red cross over their name.
// TODO: Even better, when they are doing very well, e.g. scoring frequently, indicate that they are on fire or something.
class Avatar(player: Player, private val uiAssets: UiAssets): Actor() {

    companion object {
        const val ICON_SIZE = 64f
        const val PADDING = UI_SPACE
        const val SIZE = ICON_SIZE + UI_SPACE * 2f
    }

    private val beard: TextureRegion
    private val body: TextureRegion
    private val hair: TextureRegion
    private val leg: TextureRegion
    private val torso: TextureRegion
    private var hasBeard: Boolean

    init {
        val random = Random(player.id)

        val sprites = uiAssets.getSprites()

        beard = sprites.characters.beards.random(random)
        body = sprites.characters.bodies.random(random)
        hair = sprites.characters.hair.random(random)
        leg = sprites.characters.legs.random(random)
        torso = sprites.characters.torsos.random(random)

        // We could equally just store a null in the beard property, but this way it makes it
        // easier to ensure we always have the same number of calls to the Random object, making
        // it a little bit easier to reason about the deterministic randomness
        hasBeard = random.nextFloat() < 0.2

        setBounds(0f, 0f, SIZE, SIZE)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (batch == null) {
            return
        }

        batch.draw(body, x + PADDING, y + PADDING, ICON_SIZE, ICON_SIZE)
        batch.draw(hair, x + PADDING, y + PADDING, ICON_SIZE, ICON_SIZE)
        if (hasBeard) {
            batch.draw(beard, x + PADDING, y + PADDING, ICON_SIZE, ICON_SIZE)
        }
        batch.draw(torso, x + PADDING, y + PADDING, ICON_SIZE, ICON_SIZE)
        batch.draw(leg, x + PADDING, y + PADDING, ICON_SIZE, ICON_SIZE)
    }
}

class AvatarTile(player: Player, uiAssets: UiAssets, highlight: Boolean = false): WidgetGroup() {

    init {
        val avatar = Avatar(player, uiAssets)
        val background = Button(uiAssets.getSkin(), "default").apply {
            isDisabled = true
            setFillParent(true)

            if (highlight) {
                style = Button.ButtonStyle(style)
                style.disabled = uiAssets.getSkin().getDrawable("button-over-c")
            }
        }
        val icons = uiAssets.getSprites().icons
        val gameDetails = Games.all[player.game]
        val iconSprite = if (gameDetails == null) icons.unknown else gameDetails.icon(uiAssets.getSprites())
        val icon = Image(iconSprite).apply {
            setSize(Avatar.ICON_SIZE, Avatar.ICON_SIZE)
            x = Avatar.SIZE + UI_SPACE
            y = UI_SPACE
        }

        addActor(background)
        addActor(avatar)
        addActor(icon)
    }

    override fun getPrefWidth() = Avatar.SIZE * 2
    override fun getPrefHeight() = Avatar.SIZE

}