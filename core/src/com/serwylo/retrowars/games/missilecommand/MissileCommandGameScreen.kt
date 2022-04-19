package com.serwylo.retrowars.games.missilecommand

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector2
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.missilecommand.entities.*
import kotlin.math.abs

class MissileCommandGameScreen(game: RetrowarsGame) : GameScreen(game, Games.missileCommand, "game.missile-command.intro-message.positive", "game.missile-command.intro-message.negative", 400f, 250f) {

    companion object {
        @Suppress("unused")
        const val TAG = "MissileCommandGameScreen"
    }

    private val state = MissileCommandGameState(viewport.worldWidth, viewport.worldHeight)

    init {
        queueEnemyMissile()
    }

    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(getInputProcessor(), object: InputAdapter() {

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (getState() != State.Playing) {
                    return false
                }

                val worldPos = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat()))

                val closest = state.turrets
                    .filter { it.ammunition > 0 }
                    .minByOrNull { abs(it.position.x - worldPos.x) }

                if (closest == null) {
                    // TODO: Show feedback that we are completely out of ammunition
                } else {
                    fire(closest, Vector2(worldPos.x, worldPos.y))
                }

                return true

            }

        })
    }

    private fun fire(turret: Turret, target: Vector2) {
        turret.ammunition --
        state.friendlyMissiles.add(FriendlyMissile(turret, target))
    }

    override fun updateGame(delta: Float) {
        state.timer += delta

        updateEntities(delta)

        if (getState() == State.Playing && !state.anyCitiesAlive()) {
            endGame()
        }
    }

    override fun renderGame(camera: Camera) {

        val r = game.uiAssets.shapeRenderer

        state.cities.forEach { it.render(camera, r) }
        state.turrets.forEach { it.render(camera, r) }
        Missile.renderBulk(camera, r, state.friendlyMissiles)
        Missile.renderBulk(camera, r, state.enemyMissiles, state.networkMissiles)
        Explosion.renderBulk(camera, r, state.explosions)

    }

    private fun updateEntities(delta: Float) {

        val explosions = state.explosions.iterator()
        while (explosions.hasNext()) {
            val explosion = explosions.next()
            explosion.update(delta)

            if (explosion.hasReachedMaxSize()) {
                explosions.remove()
            } else {
                val enemyMissiles = state.enemyMissiles.iterator()
                while (enemyMissiles.hasNext()) {
                    val missile = enemyMissiles.next()
                    if (missile.isColliding(explosion)) {
                        enemyMissiles.remove()
                        state.networkMissiles.remove(missile)
                        increaseScore(Missile.POINTS)
                    }
                }
            }
        }

        val friendlyMissiles = state.friendlyMissiles.iterator()
        while (friendlyMissiles.hasNext()) {
            val missile = friendlyMissiles.next()
            missile.update(delta)

            if (missile.hasReachedDestination()) {
                state.explosions.add(Explosion(missile.target))
                friendlyMissiles.remove()
            }
        }

        val enemyMissiles = state.enemyMissiles.iterator()
        while (enemyMissiles.hasNext()) {
            val missile = enemyMissiles.next()
            missile.update(delta)

            if (missile.hasReachedDestination()) {
                enemyMissiles.remove()
                state.networkMissiles.remove(missile)

                missile.targetCity.health --
            }
        }

        if (getState() == State.Playing && state.numMissilesRemaining <= 0) {
            if (state.enemyMissiles.size == 0) {
                completeLevel()
            }
        } else if (state.shouldFireEnemyMissile()) {
            fireEnemyMissile()
            queueEnemyMissile()
        }

    }

    override fun onReceiveDamage(strength: Int) {
        for (i in 0..strength * 2) {
            val missile = fireEnemyMissile()
            state.networkMissiles.add(missile)

            // These are not counted as normal missiles from this level, so extend the length of
            // the level in response.
            state.numMissilesRemaining ++
        }
    }

    private fun completeLevel() {
        with(state) {
            increaseScore(MissileCommandGameState.BONUS_SCORE_PER_LEVEL)

            nextEnemyMissileTime = timer + (MissileCommandGameState.MAX_TIME_BETWEEN_ENEMY_MISSILES * 1.5f)

            level ++
            missileSpeed = MissileCommandGameState.INITIAL_MISSILE_SPEED + (level * MissileCommandGameState.SPEED_INCREASE_PER_LEVEL)
            numMissilesRemaining =
                (MissileCommandGameState.BASE_NUM_MISSILES_FOR_LEVEL + (level * MissileCommandGameState.EXTRA_MISSILES_PER_LEVEL))
                    .coerceAtMost(MissileCommandGameState.MAX_MISSILES_PER_LEVEL)

            turrets.forEach { it.ammunition = Turret.INITIAL_AMMUNITION }

            // Don't improve the health of cities automatically at the end of the level.
            // Rather, wait for a certain amount of points to be reached and then give back a city in response.
        }
    }

    private fun fireEnemyMissile(): EnemyMissile {
        val aliveCities = state.cities.filter { it.health > 0 }

        val targetCity = if (aliveCities.isEmpty()) {
            // Don't really expect to get here, this is more defensive.
            // Elsewhere, we should be ending the game when the last city is dead. At this point,
            // we just fire missiles at random dead cities.
            state.cities.random()
        } else {
            aliveCities.random()
        }

        val startX = (Math.random() * viewport.worldWidth).toFloat()
        val missile = EnemyMissile(state.missileSpeed, Vector2(startX, viewport.worldHeight), targetCity)

        state.enemyMissiles.add(missile)
        state.numMissilesRemaining --

        return missile
    }

    private fun queueEnemyMissile() {
        val max = MissileCommandGameState.MAX_TIME_BETWEEN_ENEMY_MISSILES
        val min = MissileCommandGameState.MIN_TIME_BETWEEN_ENEMY_MISSILES
        state.nextEnemyMissileTime = state.timer + ((Math.random() * (max - min)) + min).toFloat()
    }

}
