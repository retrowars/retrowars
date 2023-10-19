package com.serwylo.retrowars

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.I18NBundleLoader
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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
class UiAssets(private val locale: Locale) {

    val shapeRenderer = ShapeRenderer()
    val spriteBatch = SpriteBatch()

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

        styles = Styles(skin, locale)

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
            manager.addEffect(RadialDistortionEffect().apply {
                distortion = 0.08f
                zoom = 0.98f
            })
            manager.addEffect(CrtEffect(CrtEffect.LineStyle.HORIZONTAL_SMOOTH, 1.3f, 0.5f).apply {
                sizeSource = CrtEffect.SizeSource.VIEWPORT
            })
            manager.addEffect(OldTvEffect().apply {
                time = 0.15f
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

                Gdx.gl20.glLineWidth(3f)
                closure()

                manager.endInputCapture()
                manager.applyEffects()
                manager.renderToScreen()
            } else {
                Gdx.gl20.glLineWidth(2f)
                closure()
            }

            Gdx.gl.glDisable(GL20.GL_BLEND)
        }

        fun resize(width: Int, height: Int) {
            manager.resize(width, height)
        }

    }

    /**
     * Depending on the [locale], we may need to use some different fonts.
     * The default font set is based on Kenney's great pixel fonts. However, they are only able to
     * maintain a character set that is so large. Therefore, to cater for a broader range of
     * characters beyond the bulk of the ASCII set, we fall back to using the Google Noto font family.
     */
    class Styles(private val skin: Skin, private val locale: Locale) {

        private val font = fontForLocale(locale) ?: Font.KENNEY

        val label = Labels()
        val textButton = TextButtons()

        inner class Labels {

            val small = skin.get("small", Label.LabelStyle::class.java)
            val medium = skin.get("default", Label.LabelStyle::class.java)
            val large = skin.get("large", Label.LabelStyle::class.java)
            val huge = skin.get("huge", Label.LabelStyle::class.java)

            init {
                if (font != Font.KENNEY) {
                    val smallAlternative = skin.get("small-${font.id}", Label.LabelStyle::class.java)
                    val mediumAlternative = skin.get("default-${font.id}", Label.LabelStyle::class.java)
                    val largeAlternative = skin.get("large-${font.id}", Label.LabelStyle::class.java)
                    val hugeAlternative = skin.get("huge-${font.id}", Label.LabelStyle::class.java)

                    huge.font = hugeAlternative.font
                    large.font = largeAlternative.font
                    medium.font = mediumAlternative.font
                    small.font = smallAlternative.font
                }
            }
        }

        inner class TextButtons {
            val small = skin.get("small", TextButton.TextButtonStyle::class.java)
            val medium = skin.get("default", TextButton.TextButtonStyle::class.java)
            val large = skin.get("large", TextButton.TextButtonStyle::class.java)
            val huge = skin.get("huge", TextButton.TextButtonStyle::class.java)

            init {
                if (font != Font.KENNEY) {

                    val smallAlternative = skin.get("small-${font.id}", TextButton.TextButtonStyle::class.java)
                    val mediumAlternative = skin.get("default-${font.id}", TextButton.TextButtonStyle::class.java)
                    val largeAlternative = skin.get("large-${font.id}", TextButton.TextButtonStyle::class.java)
                    val hugeAlternative = skin.get("huge-${font.id}", TextButton.TextButtonStyle::class.java)

                    huge.font = hugeAlternative.font
                    large.font = largeAlternative.font
                    medium.font = mediumAlternative.font
                    small.font = smallAlternative.font
                }
            }
        }
    }

    class Sprites(private val atlas: TextureAtlas) {

        val icons = Icons()
        val buttonIcons = ButtonIcons()
        val characters = Characters()
        val gameSprites = GameSprites()

        inner class GameSprites {
            val space_invaders_barrier = atlas.findRegion("space_invaders_barrier")!!
        }

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
            val breakout = atlas.findRegion("icon_breakout")!!
            val missileCommand = atlas.findRegion("icon_missile_command")!!
            val snake = atlas.findRegion("icon_snake")!!
            val spaceInvaders = atlas.findRegion("icon_space_invaders")!!
            val tempest = atlas.findRegion("icon_tempest")!!
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
            val fire = atlas.findRegion("fire")!!
            val rotate_clockwise = atlas.findRegion("rotate_clockwise")!!
            val rotate_counter_clockwise = atlas.findRegion("rotate_counter_clockwise")!!
            val music_off = atlas.findRegion("music_off")!!
            val music_on = atlas.findRegion("music_on")!!
            val audio_off = atlas.findRegion("audio_off")!!
            val audio_off_a = atlas.findRegion("audio_off_a")!!
            val audio_off_b = atlas.findRegion("audio_off_b")!!
            val audio_on = atlas.findRegion("audio_on")!!
        }

    }

    enum class Font(val id: String) {
        KENNEY("kenney"),
        NOTO_MONO("noto_mono"),
        NOTO_CJK("noto_cjk"),
    }

    companion object {

        private const val TAG = "UiAssets"

        /**
         * Unfortunately despite the awesome effort of community translators, some languages
         * are yet to be supported.
         *
         * Part of that is something we can solve, by choosing a bitmap font with a wider set of
         * characters. This can be done, e.g. using tools such as Heiro (https://github.com/libgdx/libgdx/wiki/Hiero)
         * and fonts such as Noto. Using such a font will cover a bigger range of characters, but
         * lose the nice styles provided by the Kenney fonts used now. Therefore, it would be great
         * to only enable these fonts for languages which require them, and leave Kenney fonts for others.
         *
         * The other part is an issue with libgdx, whereby it doesn't natively support RTL languages,
         * or languages where glyphs are combined together such as Persian. This will be harder to
         * accomplish unfortunately.
         */
        val supportedLocales = mapOf(
            "bs" to Font.NOTO_MONO, // ž and other characters not represented by Kenney.
            "de" to Font.KENNEY,
            "en" to Font.KENNEY,
            "es" to Font.KENNEY,
            "et" to Font.KENNEY,
            "fr" to Font.KENNEY,
            "hu" to Font.NOTO_MONO, // ő and perhaps others are not supported by Kenney.
            "hr" to Font.NOTO_MONO, // ž and other characters not represented by Kenney.
            "id" to Font.KENNEY,
            "in" to Font.KENNEY, // Java and Weblate disagree on the language code for Indonesian. Java uses "in", weblate uses "id", so we symlink them together.
            "it" to Font.KENNEY,
            "ko" to Font.NOTO_CJK,
            "nb" to Font.KENNEY,
            "nl" to Font.KENNEY,
            "pl" to Font.NOTO_MONO,
            "pt" to Font.KENNEY, // TODO: This is for pt_BR. If pt translation lands, then update code to support countries. Currently just supports language.
            "ru" to Font.NOTO_MONO,
            "sr" to Font.NOTO_MONO,
            "tr" to Font.NOTO_MONO,
            "uk" to Font.NOTO_MONO,
        )

        private fun fontForLocale(locale: Locale): Font? {
            val country = locale.language.toLowerCase(Locale.ENGLISH)
            return supportedLocales[country]
        }

        fun getLocale(localeFromCli: Locale?): Locale {
            // Even though Weblate is allowing this game to be translated into many different languages,
            // only some of them are supported by libgdx. Ensure that we don't pick up an unsupported locale
            // which *does* have translation files available, because it will render invalid glyphs and
            // make the game unusable.
            val preferredLocale = localeFromCli ?: Locale.getDefault()

            val font = fontForLocale(preferredLocale)

            return if (font != null) {
                preferredLocale
            } else {
                Gdx.app.error(TAG, "Unsupported locale: $preferredLocale, falling back to English.")
                Locale.ROOT
            }
        }

    }

}
