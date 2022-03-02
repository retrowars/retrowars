package com.serwylo.retrowars.games.spaceinvaders

import java.util.*


class SpaceInvadersGameState(worldWidth: Float, private val worldHeight: Float) {

    companion object {
        const val PLAYER_SPEED = 100f
        const val TIME_BETWEEN_ENEMY_STEP = 0.05f
        const val PLAYER_BULLET_SPEED = 150f
        const val ENEMY_BULLET_SPEED = 150f
    }

    val cellWidth = worldWidth / 20f
    val cellHeight = worldHeight / 20f
    val padding = cellWidth / 5f
    val enemyStepSize = cellWidth / 4

    var timer = 0f
    var timeUntilEnemyStep = TIME_BETWEEN_ENEMY_STEP

    var playerX = worldWidth / 2f

    var isMovingLeft = false
    var isMovingRight = false
    var isFiring = false

    var playerBullet: Bullet? = null
    val enemyBullets = LinkedList<Bullet>()

    var enemyDirection = Direction.Right

    var enemies: List<List<Enemy>> = spawnEnemies()

    var movingRow = enemies.size - 1

        private fun spawnEnemies() = (0 until 5).map { y ->
        (0 until 11).map { x ->
            Enemy(
                x = x * (padding + cellWidth) + padding,
                y = worldHeight - cellHeight - y * (padding + cellHeight) - padding,
            )
        }
    }

}

data class Enemy(
    var x: Float,
    var y: Float,
)

data class Bullet(
    var x: Float,
    var y: Float,
)

enum class Direction {
    Left,
    Right,
}