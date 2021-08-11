package com.serwylo.retrowars

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.I18NBundleLoader
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.I18NBundle
import com.crashinvaders.vfx.VfxManager
import com.crashinvaders.vfx.effects.*
import com.gmail.blueboxware.libgdxplugin.annotations.GDXAssets
import com.serwylo.retrowars.utils.Options
import java.util.*

@Suppress("PropertyName") // Allow underscores in variable names here, because it better reflects the source files things come from.
class UiAssets(locale: Locale) {

    val shapeRenderer = ShapeRenderer()

    private val manager = AssetManager()
    private lateinit var skin: Skin
    private lateinit var styles: Styles
    private lateinit var sprites: Sprites
    private lateinit var effects: Effects

    @GDXAssets(propertiesFiles = ["android/assets/i18n/messages.properties"])
    private lateinit var strings: I18NBundle

    init {
        manager.load("i18n/messages", I18NBundle::class.java, I18NBundleLoader.I18NBundleParameter(locale))
        manager.load("skin.json", Skin::class.java)
        manager.load("sprites.atlas", TextureAtlas::class.java)
    }

    fun initSync() {

        val startTime = System.currentTimeMillis()
        Gdx.app.debug(TAG, "Loading assets...")

        manager.finishLoading()

        strings = manager.get("i18n/messages")
        skin = manager.get("skin.json")
        sprites = Sprites(manager.get("sprites.atlas"))
        effects = Effects()

        styles = Styles(skin)

        Gdx.app.debug(TAG, "Finished loading assets (${System.currentTimeMillis() - startTime}ms)")

    }

    fun getStrings() = strings
    fun getSkin() = skin
    fun getStyles() = styles
    fun getSprites() = sprites
    fun getEffects() = effects

    class Effects {

        private val manager = VfxManager(Pixmap.Format.RGBA8888)

        init {
            manager.addEffect(FilmGrainEffect())
            manager.addEffect(GaussianBlurEffect().apply {
                amount = 0.8f
            })
            manager.addEffect(RadialDistortionEffect().apply {
                distortion = 0.08f
                zoom = 0.98f
            })
        }

        fun render(closure: () -> Unit) {

            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            if (Options.useVisualEffects()) {
                manager.cleanUpBuffers()
                manager.beginInputCapture()

                closure()

                manager.endInputCapture()
                manager.applyEffects()
                manager.renderToScreen()
            } else {
                closure()
            }

            Gdx.gl.glDisable(GL20.GL_BLEND)
        }

        fun resize(width: Int, height: Int) {
            manager.resize(width, height)
        }

    }

    class Styles(private val skin: Skin) {
        val label = Labels()
        val textButton = TextButtons()

        inner class Labels {
            val small = skin.get("small", Label.LabelStyle::class.java)
            val medium = skin.get("default", Label.LabelStyle::class.java)
            val large = skin.get("large", Label.LabelStyle::class.java)
            val huge = skin.get("huge", Label.LabelStyle::class.java)
        }

        inner class TextButtons {
            val small = skin.get("small", TextButton.TextButtonStyle::class.java)
            val medium = skin.get("default", TextButton.TextButtonStyle::class.java)
            val large = skin.get("large", TextButton.TextButtonStyle::class.java)
            val huge = skin.get("huge", TextButton.TextButtonStyle::class.java)
        }
    }

    class Sprites(private val atlas: TextureAtlas) {

        val icons = Icons()
        val buttonIcons = ButtonIcons()
        val characters = Characters()

        inner class Characters {
            val overlay_dead = atlas.findRegion("overlay_dead")!!
            val beards = (0..19).toList().map { i -> atlas.findRegion("beard_${i}_")!! }
            val bodies = (0..7).toList().map { i -> atlas.findRegion("body_${i}_")!! }
            val hair = (0..59).toList().map { i -> atlas.findRegion("hair_${i}_")!! }
            val legs = (0..19).toList().map { i -> atlas.findRegion("legs_${i}_")!! }
            val torsos = (0..119).toList().map { i -> atlas.findRegion("torso_${i}_")!! }
        }

        inner class Icons {
            val retrowars = atlas.findRegion("icon_app")!!
            val asteroids = atlas.findRegion("icon_asteroids")!!
            val missileCommand = atlas.findRegion("icon_missile_command")!!
            val snake = atlas.findRegion("icon_snake")!!
            val tetris = atlas.findRegion("icon_tetris")!!
            val unknown = atlas.findRegion("icon_unknown")!!
        }

        inner class ButtonIcons {
            val right = atlas.findRegion("arrow_right")!!
            val left = atlas.findRegion("arrow_left")!!
            val up = atlas.findRegion("arrow_up")!!
            val down = atlas.findRegion("arrow_down")!!
            val thrust = atlas.findRegion("thrust")!!
            val drop = atlas.findRegion("drop")!!
            val button_x = atlas.findRegion("button_x")!!
            val rotate_clockwise = atlas.findRegion("rotate_clockwise")!!
            val rotate_counter_clockwise = atlas.findRegion("rotate_counter_clockwise")!!
        }

    }

    companion object {

        private const val TAG = "UiAssets"

    }

}
