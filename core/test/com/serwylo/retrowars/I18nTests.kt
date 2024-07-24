package com.serwylo.retrowars

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import de.tomgrill.gdxtesting.GdxTestRunner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(GdxTestRunner::class)
class I18nTests {

    @Test
    fun testStringFormats() {
        UiAssets.supportedLocales.forEach { (locale) ->
            val strings = I18NBundle.createBundle(Gdx.files.internal("i18n/messages"), Locale(locale))

            val props = Properties().apply {
                load(Gdx.files.internal("i18n/messages.properties").read())
            }

            val regex = Regex(".*\\{\\d+,choice,.*")
            val toCheck = props.entries.filter {
                it.value.toString().matches(regex)
            }.map { it.key.toString() }

            toCheck.forEach {
                Gdx.app.log("I18nTests", "Locale: \"$locale\", String: \"$it\"")
                val string = strings.format(it, 1)

                assertNotNull(string)
                assertNotEquals("", string)
            }
        }

    }

}
