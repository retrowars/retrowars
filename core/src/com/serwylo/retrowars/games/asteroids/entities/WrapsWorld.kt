package com.serwylo.retrowars.games.asteroids.entities

import com.badlogic.gdx.math.Vector2

interface WrapsWorld {

    var worldWidth: Float
    var worldHeight: Float

    fun setWorldSize(width: Float, height: Float) {
        worldWidth = width
        worldHeight = height
    }

    fun isFullyPastRight(left: Float) = left > worldWidth
    fun isPartiallyPastRight(right: Float) = right > worldWidth

    fun isFullyPastTop(bottom: Float) = bottom > worldHeight
    fun isPartiallyPastTop(top: Float) = top > worldHeight

    fun isFullyPastLeft(right: Float) = right < 0
    fun isPartiallyPastLeft(left: Float) = left < 0

    fun isFullyPastBottom(top: Float) = top < 0
    fun isPartiallyPastBottom(bottom: Float) = bottom < 0

    fun isFullyPastRight(): Boolean
    fun isPartiallyPastRight(): Boolean

    fun isFullyPastTop(): Boolean
    fun isPartiallyPastTop(): Boolean

    fun isFullyPastLeft(): Boolean
    fun isPartiallyPastLeft(): Boolean

    fun isFullyPastBottom(): Boolean
    fun isPartiallyPastBottom(): Boolean

    fun maybeWrapPosition(position: Vector2) {

        if (isFullyPastBottom()) {
            position.y += worldHeight
        } else if (isFullyPastTop()) {
            position.y -= worldHeight
        }

        if (isFullyPastLeft()) {
            position.x += worldWidth
        } else if (isFullyPastRight()) {
            position.x -= worldWidth
        }

    }

}
