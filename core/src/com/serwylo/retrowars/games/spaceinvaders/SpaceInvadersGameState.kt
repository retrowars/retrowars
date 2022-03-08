package com.serwylo.retrowars.games.spaceinvaders

import java.util.*


class SpaceInvadersGameState(worldWidth: Float, private val worldHeight: Float) {

    companion object {

        const val PAUSE_AFTER_DEATH = 3f

        const val PLAYER_SPEED = 100f

        const val SCORE_PER_ENEMY = 2000

        const val MAX_ENEMY_BULLETS_ON_SCREEN = 3

        /**
         * Watching the original, it seems like after a bullet hits something and explodes, there
         * are then 6 frames of waiting before the next bullet is launched.
         */
        const val DELAY_AFTER_ENEMY_FIRE = 6f / 30f

        const val INITIAL_DELAY_ENEMY_FIRE = DELAY_AFTER_ENEMY_FIRE * 5

        const val TIME_BETWEEN_LEVELS = 1.5f

        /**
         * From watching videos of the original space invaders, I can see that a row of enemies
         * shuffles across two enemies at a time, and initially they move once each frame (1/30 sec).
         * The entire row thus takes 7/30 ths of a seconds to traverse the screen
         */
        const val TIME_BETWEEN_ENEMY_STEP = 7f / 30f

        private const val NUM_ENEMIES_PER_ROW = 11
        private const val NUM_ENEMY_ROWS = 5

        /**
         * In the original, we have:
         *  - Top row: 8 pixels wide: https://spaceinvaders.fandom.com/wiki/Squid_(Small_Invader)
         *  - Middle row: 11 pixels wide: https://spaceinvaders.fandom.com/wiki/Crab_(Medium_Invader)
         *  - Bottom 2 rows: 12 pixels wide: https://spaceinvaders.fandom.com/wiki/Octopus_(Large_Invader)
         */
        private val ROW_WIDTHS = listOf(
            8f / 12f,
            11f / 12f,
            11f / 12f,
            1f,
            1f
        )

    }

    val cellWidth = worldWidth / 20f
    val cellHeight = worldHeight / 20f
    val padding = cellWidth / 5f
    val bulletHeight = padding * 2
    val bulletWidth = padding / 4

    /**
     * 18 steps across per level. Find the remaining space and divide by 18.
     */
    val enemyStepSize = (worldWidth - (NUM_ENEMIES_PER_ROW * cellWidth) - ((NUM_ENEMIES_PER_ROW + 1) * padding)) / 18f

    /**
     * Just under 1 second to traverse the screen height.
     */
    val playerBulletSpeed = (worldHeight - cellHeight - padding * 2) * 26f / 30f

    /**
     * This is approximate, because the enemy never shots from one end of the screen. Rather, we
     * measure how long it takes for an enemy on the starting row to hit the ground, then we double
     * it as it is approximately halfway down the screen.
     */
    val enemyBulletSpeed = (worldHeight - padding * 2) * 30f / 80f

    var timer = 0f
    var timeUntilEnemyStep = TIME_BETWEEN_ENEMY_STEP
    var timeUntilEnemyFire = INITIAL_DELAY_ENEMY_FIRE
    var nextLevelTime = -1f

    var numLives = 3
    var nextPlayerRespawnTime = -1f

    var playerX = cellWidth / 2f + padding

    var isMovingLeft = false
    var isMovingRight = false
    var isFiring = false

    var playerBullet: Bullet? = null
    val enemyBullets = LinkedList<Bullet>()

    var enemyDirection = Direction.Right

    var enemies: List<EnemyRow> = spawnEnemies()

    var movingRow = enemies.size - 1

    fun spawnEnemies() = (0 until NUM_ENEMY_ROWS).map { y ->
        EnemyRow(
            y = worldHeight - cellHeight - y * (padding + cellHeight) - padding,
            enemies = (0 until NUM_ENEMIES_PER_ROW).map { x ->
                val enemyWidth = cellWidth * (ROW_WIDTHS.getOrNull(y) ?: 1f)
                val offsetFromCell = (cellWidth - enemyWidth) / 2
                Enemy(
                    x = x * (padding * 1.5f + cellWidth) + padding + offsetFromCell,
                    width = enemyWidth,
                )
            }.toMutableList(),
        )
    }

}

data class EnemyRow(
    var y: Float,
    val enemies: MutableList<Enemy>,
)

data class Enemy(
    var x: Float,
    val width: Float,
)

data class Bullet(
    var x: Float,
    var y: Float,
)

enum class Direction {
    Left,
    Right,
}