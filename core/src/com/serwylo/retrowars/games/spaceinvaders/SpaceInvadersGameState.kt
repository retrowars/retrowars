package com.serwylo.retrowars.games.spaceinvaders

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import java.util.*


class SpaceInvadersGameState(
    worldWidth: Float,
    private val worldHeight: Float,
    barrierTexture: TextureRegion,
) {

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
        const val TIME_BETWEEN_ENEMY_STEP_SLOWEST = 7f / 30f

        /**
         * When only one enemy is left, it takes a step every frame.
         */
        const val TIME_BETWEEN_ENEMY_STEP_FASTEST = 1f / 30f

        const val NUM_ENEMIES_PER_ROW = 11
        const val NUM_ENEMY_ROWS = 5

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

        val playerBulletExplosion = ExplosionPattern("""
            x   x  x
              x   x
             xxxxxx
            xxxxxxxx
            xxxxxxxx
             xxxxxx
                Ox
                   x
        """)

        val enemyBulletExplosion = ExplosionPattern("""
            x
              xx  
             xxOxx
            x xxx 
             xxxxx
            x xxx
             x x x
         """)

    }

    val cellWidth = worldWidth / 20f
    val cellHeight = worldHeight / 20f
    val padding = cellWidth / 5f
    val bulletHeight = padding * 2
    val bulletWidth = padding / 4

    val barriers = (1 until 5).map { x ->
        val data = barrierTexture.texture.textureData
        if (!data.isPrepared) {
            data.prepare()
        }

        val pixmap = Pixmap(barrierTexture.regionWidth, barrierTexture.regionHeight, data.format).also { pixmap ->
            pixmap.drawPixmap(data.consumePixmap(), 0, 0, barrierTexture.regionX, barrierTexture.regionY, barrierTexture.regionWidth, barrierTexture.regionHeight)
        }

        val barrierX = worldWidth / 5 * x
        Barrier(
            pixmap,
            barrierX - cellWidth,
            padding * 2 + cellHeight * 2,
            cellWidth * 2,
            cellHeight * 2,
        )
    }

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
    var timeUntilEnemyStep = TIME_BETWEEN_ENEMY_STEP_SLOWEST
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

data class Barrier(
    /**
     * The destructive barrier is modelled by pixels in this pixmap. As it gets destroyed, pixels
     * are set to black. Collision detection is based on pixel collisions.
     */
    val pixmap: Pixmap,

    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,

)

class ExplosionPattern(patternString: String) {
    val pattern: List<List<Boolean>>
    val originX: Int
    val originY: Int

    init {
        val chars = patternString
            .trimIndent()
            .split("\n")
            .map { line ->
                line.toCharArray()
            }

        var ox = 0
        var oy = 0
        pattern = chars.mapIndexed { y, row ->
            row.mapIndexed { x, cell ->
                if (cell == 'O') {
                    ox = x
                    oy = y
                }

                cell != ' '
            }
        }

        originX = ox
        originY = oy

    }
}
