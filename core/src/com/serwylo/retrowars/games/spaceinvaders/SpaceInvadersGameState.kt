package com.serwylo.retrowars.games.spaceinvaders

import java.util.*


class SpaceInvadersGameState(worldWidth: Float, private val worldHeight: Float) {

    companion object {
        const val PLAYER_SPEED = 100f
        const val TIME_BETWEEN_ENEMY_STEP = 0.05f
        const val PLAYER_BULLET_SPEED = 150f
        const val ENEMY_BULLET_SPEED = 150f

        const val MIN_TIME_BETWEEN_ENEMY_FIRE = 3f
        const val MAX_TIME_BETWEEN_ENEMY_FIRE = 6f
        const val MAX_ENEMY_BULLETS_ON_SCREEN = 3

        private const val NUM_ENEMIES_PER_ROW = 11
        private const val NUM_ENEMY_ROWS = 5
    }

    val cellWidth = worldWidth / 20f
    val cellHeight = worldHeight / 20f
    val padding = cellWidth / 5f
    val bulletHeight = padding * 2
    val bulletWidth = padding / 2
    val enemyStepSize = cellWidth / 4

    var timer = 0f
    var timeUntilEnemyStep = TIME_BETWEEN_ENEMY_STEP
    var timeUntilEnemyFire = (Math.random() * (MAX_TIME_BETWEEN_ENEMY_FIRE - MIN_TIME_BETWEEN_ENEMY_FIRE) + MIN_TIME_BETWEEN_ENEMY_FIRE).toFloat()

    var playerX = worldWidth / 2f

    var isMovingLeft = false
    var isMovingRight = false
    var isFiring = false

    var playerBullet: Bullet? = null
    val enemyBullets = LinkedList<Bullet>()

    var enemyDirection = Direction.Right

    var enemies: List<EnemyRow> = spawnEnemies()

    var movingRow = enemies.size - 1

    private fun spawnEnemies() = (0 until NUM_ENEMY_ROWS).map { y ->
        EnemyRow(
            y = worldHeight - cellHeight - y * (padding + cellHeight) - padding,
            enemies = (0 until NUM_ENEMIES_PER_ROW).map { x -> Enemy(x * (padding + cellWidth) + padding) }.toMutableList(),
        )
    }

}

data class EnemyRow(
    var y: Float,
    val enemies: MutableList<Enemy>,
)

data class Enemy(
    var x: Float,
)

data class Bullet(
    var x: Float,
    var y: Float,
)

enum class Direction {
    Left,
    Right,
}