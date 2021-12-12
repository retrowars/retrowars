package com.serwylo.beatgame.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.games.GameDetails
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

fun makeHeading(title: String, styles: UiAssets.Styles, strings: I18NBundle, onBack: (() -> Unit)? = null) =
    makeHeading(icon = null, title, styles, strings, onBack)

fun makeHeading(icon: TextureRegion?, title: String, styles: UiAssets.Styles, strings: I18NBundle, onBack: (() -> Unit)? = null): HorizontalGroup {
    return HorizontalGroup().apply {
        space(UI_SPACE * 2)
        padBottom(UI_SPACE)

        if (icon != null) {
            addActor(makeIcon(icon, UI_SPACE * 8))
        }

        addActor(Label(title, styles.label.huge))
        if (onBack != null) {
            addActor(makeSmallButton(strings["btn.back"], styles, onBack))
        }
    }
}

fun withTransparentBackground(actor: Actor, skin: Skin) =
    Window("", skin, "default").apply {
        add(actor)
            .padTop(UI_SPACE)
            .padBottom(UI_SPACE)
            .padLeft(UI_SPACE * 4)
            .padRight(UI_SPACE * 4)
    }

fun withBackground(actor: Actor, skin: Skin) =
    Window("", skin, "transparent-text-background").apply {
        add(actor)
            .padTop(UI_SPACE)
            .padBottom(UI_SPACE)
            .padLeft(UI_SPACE * 4)
            .padRight(UI_SPACE * 4)
    }

val UI_WIDTH = 800f
val UI_HEIGHT = 600f
const val UI_SPACE = 10f

class Avatar(playerId: Long, uiAssets: UiAssets, var isDead: Boolean = false): Actor() {

    companion object {
        const val ICON_SIZE = 64f
        const val PADDING = UI_SPACE
        const val SIZE = ICON_SIZE + UI_SPACE * 2f
    }

    private val sprites = uiAssets.getSprites()

    private val beard: TextureRegion
    private val body: TextureRegion
    private val hair: TextureRegion
    private val leg: TextureRegion
    private val torso: TextureRegion
    private var hasBeard: Boolean

    init {
        val random = Random(playerId)

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

        if (isDead) {
            batch.draw(sprites.characters.overlay_dead, x + PADDING, y + PADDING, ICON_SIZE, ICON_SIZE)
        }
    }
}

fun makeAvatarAndGameIcon(playerId: Long, isDead: Boolean, gameDetails: GameDetails, uiAssets: UiAssets) = Stack().also { stack ->
    stack.add(HorizontalGroup().also { wrapper ->
        wrapper.padLeft(UI_SPACE * 6)
        wrapper.addActor(makeGameIcon(gameDetails, uiAssets).apply {
            name = "game"
        })
    })

    stack.add(HorizontalGroup().also { wrapper ->
        wrapper.padRight(UI_SPACE * 5)
        wrapper.addActor(Avatar(playerId, uiAssets, isDead).apply {
            name = "avatar"
        })
    })
}

fun makeGameIcon(gameDetails: GameDetails, uiAssets: UiAssets): Image {

    val iconSprite = gameDetails.icon(uiAssets.getSprites())

    return Image(iconSprite).apply {
        setSize(Avatar.ICON_SIZE, Avatar.ICON_SIZE)
    }

}

object CustomActions {

    fun bounce(numJumps: Int = 5): Action =
        Actions.repeat(
            numJumps, Actions.sequence(
                Actions.moveBy(0f, 10f, 0.15f, Interpolation.bounceOut),
                Actions.moveBy(0f, -10f, 0.15f, Interpolation.bounceIn)
            )
        )

}