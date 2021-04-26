package com.serwylo.retrowars.games.missilecommand

import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.missilecommand.entities.*
import java.util.*

class MissileCommandGameState(worldWidth: Float, worldHeight: Float) {

    companion object {
        const val MIN_TIME_BETWEEN_ENEMY_MISSILES = 0.5f
        const val MAX_TIME_BETWEEN_ENEMY_MISSILES = 3f
    }

    var score: Long = 0

    /**
     * Seconds elapsed since the game began. Wont count during pause.
     */
    var timer = 0f

    var nextEnemyMissileTime = -1f

    val cities: List<City>
    val turrets: List<Turret>

    val friendlyMissiles = LinkedList<FriendlyMissile>()
    val enemyMissiles = LinkedList<EnemyMissile>()
    val explosions = LinkedList<Explosion>()

    init {

        /*
         *       Turret   City    City    City   Turret   City    City    City   Turret
         * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
         * 0       1       2       3       4       5       6       7       8       9       10
         */

        val segments = worldWidth / 10f

        cities = listOf(
            City(Vector2(2 * segments, 0f)),
            City(Vector2(3 * segments, 0f)),
            City(Vector2(4 * segments, 0f)),

            City(Vector2(6 * segments, 0f)),
            City(Vector2(7 * segments, 0f)),
            City(Vector2(8 * segments, 0f))
        )

        turrets = listOf(
            Turret(Vector2(1 * segments, 0f)),
            Turret(Vector2(5 * segments, 0f)),
            Turret(Vector2(9 * segments, 0f))
        )
    }

    fun shouldFireEnemyMissile() = timer > nextEnemyMissileTime
    fun anyCitiesAlive() = cities.any { it.health > 0 }

}
