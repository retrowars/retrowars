package com.serwylo.retrowars.scoring

import com.badlogic.gdx.Gdx

object Options {

    private var useVisualEffects: Boolean = prefs().getBoolean("useVisualEffects", true)

    fun useVisualEffects(): Boolean {
        return useVisualEffects
    }

    fun setUseVisualEffects(use: Boolean) {
        prefs().putBoolean("useVisualEffects", use)
        useVisualEffects = use
    }

    private fun prefs() = Gdx.app.getPreferences("com.serwylo.retrowars.options")

}