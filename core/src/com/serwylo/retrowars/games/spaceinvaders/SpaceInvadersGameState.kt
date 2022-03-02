package com.serwylo.retrowars.games.spaceinvaders

class SpaceInvadersGameState(worldWidth: Float, worldHeight: Float) {

    companion object {
        const val PLAYER_SPEED = 100f
    }

    val cellWidth = worldWidth / 20f
    val cellHeight = worldWidth / 20f
    val padding = cellWidth / 5f

    var timer = 0f

    var playerX = worldWidth / 2f

    var isMovingLeft = false
    var isMovingRight = false
    var isFiring = false

}