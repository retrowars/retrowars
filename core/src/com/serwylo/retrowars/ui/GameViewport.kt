package com.serwylo.retrowars.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport

/**
 * Takes into account the fact that our GameHUD shows information in the bottom of the screen.
 * Therefore, it will always ensure to reserve that amount of space and not draw there.
 *
 * Note: Will need to apply this viewport prior to rendering game things, and reset to the
 * regular full screen viewport after so that Scene2d and our HUD can render appropriately.
 */
class GameViewport(minWorldWidth: Float, minWorldHeight: Float, camera: Camera) : ExtendViewport(minWorldWidth, minWorldHeight, camera) {

    companion object {
        const val BOTTOM_OFFSET_SCREEN_PROPORTION = 1f / 10f
        const val BOTTOM_OFFSET_MAX_SCREEN_PX = 150
        const val BOTTOM_OFFSET_MIN_SCREEN_PX = 50
    }

    private var bottomOffset = 0

    fun getBottomOffset() = bottomOffset

    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        bottomOffset = (screenHeight * BOTTOM_OFFSET_SCREEN_PROPORTION).toInt().coerceIn(BOTTOM_OFFSET_MIN_SCREEN_PX, BOTTOM_OFFSET_MAX_SCREEN_PX)

        super.update(screenWidth, screenHeight - bottomOffset, centerCamera)

        setScreenBounds(0, bottomOffset, screenWidth ,screenHeight - bottomOffset)
    }

    fun renderIn(render: () -> Unit) {
        // Note: This call to apply() will not work very well with PerspectiveCamera's, because the
        // Z position will be forceably set to 0f which usually isn't desired. However, it is likely
        // better for the game to manage its own camera if it needs a PerspectiveCamera anyway.
        // See Tempest for an example of how this is done in the render method.
        apply()
        render()
        HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    }

}
