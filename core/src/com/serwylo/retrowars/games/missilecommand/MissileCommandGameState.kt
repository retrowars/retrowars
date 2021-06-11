package com.serwylo.retrowars.games.missilecommand

import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.games.missilecommand.entities.*
import java.util.*

class MissileCommandGameState(worldWidth: Float, worldHeight: Float) {

    companion object {
        const val MIN_TIME_BETWEEN_ENEMY_MISSILES = 0.5f
        const val MAX_TIME_BETWEEN_ENEMY_MISSILES = 3f
        const val INITIAL_MISSILE_SPEED = 25f
        const val SPEED_INCREASE_PER_LEVEL = 0.25f

        // TODO: Just make this dependent on how many missiles are left. A conservative player who
        //       is more careful with their missiles will be rewarded more handsomely
        const val BONUS_SCORE_PER_LEVEL = Missile.POINTS * 5

        const val BASE_NUM_MISSILES_FOR_LEVEL = 6 * City.INITIAL_HEALTH // 1 per city.
        const val EXTRA_MISSILES_PER_LEVEL = 3

        // Force multiple missiles to be taken out together.
        // By the time we hit this limit, the speed of enemy missiles should have picked up quite a bit, which
        // should theoretically mean the same blast radius of a friendly missile will be able to
        // take care of a greater number of incoming missiles.
        const val MAX_MISSILES_PER_LEVEL = 3 *  Turret.INITIAL_AMMUNITION * 2
    }

    /**
     * Seconds elapsed since the game began. Wont count during pause.
     */
    var timer = 0f

    var nextEnemyMissileTime = -1f

    var level = 1
    var missileSpeed = INITIAL_MISSILE_SPEED + (SPEED_INCREASE_PER_LEVEL * level)
    var numMissilesRemaining = BASE_NUM_MISSILES_FOR_LEVEL + (EXTRA_MISSILES_PER_LEVEL * level)

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
            Turret(Vector2(1 * segments, 0f), 150f),
            Turret(Vector2(5 * segments, 0f), 300f),
            Turret(Vector2(9 * segments, 0f), 150f)
        )
    }

    fun shouldFireEnemyMissile() = timer > nextEnemyMissileTime
    fun anyCitiesAlive() = cities.any { it.health > 0 }

}
