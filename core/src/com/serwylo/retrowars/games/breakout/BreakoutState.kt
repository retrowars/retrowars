package com.serwylo.retrowars.games.breakout

class BreakoutState(worldWidth: Float, worldHeight: Float) {

    var targetX: Float? = null
    var paddleX: Float = worldWidth / 2f

    val blockWidth = worldWidth / 16f
    val blockHeight = blockWidth / 4f
    var paddleWidth = blockWidth * 2f
    val paddleSpeed = worldWidth

}