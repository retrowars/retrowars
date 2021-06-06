package com.serwylo.retrowars.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

class IconButton(skin: Skin, icon: TextureRegion): ImageButton(skin) {
    init {
        style = ImageButtonStyle(style)
        style.imageUp = TextureRegionDrawable(icon)
    }
}