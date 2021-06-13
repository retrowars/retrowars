package com.serwylo.retrowars.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

class IconButton(skin: Skin, icon: TextureRegion, onPress: (() -> Unit)? = null): ImageButton(skin) {
    init {
        style = ImageButtonStyle(style)
        style.imageUp = TextureRegionDrawable(icon)

        if (onPress != null) {
            addListener(object: ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    onPress()
                }
            })
        }
    }
}