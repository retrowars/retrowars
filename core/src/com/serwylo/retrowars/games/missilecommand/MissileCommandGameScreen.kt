package com.serwylo.retrowars.games.missilecommand

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.missilecommand.entities.*
import com.serwylo.retrowars.net.RetrowarsClient
import kotlin.math.abs

class MissileCommandGameScreen(private val game: RetrowarsGame) : Screen {

    companion object {
        const val MIN_WORLD_WIDTH = 400f
        const val MIN_WORLD_HEIGHT = 400f

        @Suppress("unused")
        const val TAG = "MissileCommandGameScreen"
    }

    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, camera)

    private val state: MissileCommandGameState

    private val hud: HUD

    private val client = RetrowarsClient.get()

    init {
        viewport.apply(true)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        state = MissileCommandGameState(viewport.worldWidth, viewport.worldHeight)
        queueEnemyMissile()

        hud = HUD(state, game.uiAssets)
    }

    override fun show() {
        Gdx.input.inputProcessor = object: InputAdapter() {

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {

                val worldPos = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))

                val closest = state.turrets
                    .filter { it.ammunition > 0 }
                    .minBy { abs(it.position.x - worldPos.x) }

                if (closest == null) {
                    // TODO: Show feedback that we are completely out of ammunition
                } else {
                    fire(closest, Vector2(worldPos.x, worldPos.y))
                }

                return true

            }

        }
    }

    private fun fire(turret: Turret, target: Vector2) {
        turret.ammunition --
        state.friendlyMissiles.add(FriendlyMissile(turret, target))
    }

    override fun render(delta: Float) {

        state.timer += delta

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        Gdx.graphics.gL20.glClearColor(0f, 0f, 0f, 1f)
        Gdx.graphics.gL20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        updateEntities(delta)
        renderEntities()

        hud.render(delta)

        Gdx.gl.glDisable(GL20.GL_BLEND)

    }

    private fun renderEntities() {

        val r = game.uiAssets.shapeRenderer

        state.cities.forEach { it.render(camera, r) }
        state.turrets.forEach { it.render(camera, r) }
        Missile.renderBulk(camera, r, state.friendlyMissiles)
        Missile.renderBulk(camera, r, state.enemyMissiles)
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
                        incrementScore(Missile.POINTS)
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

                missile.targetCity.health --

                if (!state.anyCitiesAlive()) {
                    // TODO: Record high score, show end of game screen.
                    game.showGameSelectMenu()
                }
            }
        }

        if (state.numMissilesRemaining <= 0) {
            if (state.enemyMissiles.size == 0) {
                completeLevel()
            }
        } else if (state.shouldFireEnemyMissile()) {
            fireEnemyMissile()
            queueEnemyMissile()
        }

    }

    private fun completeLevel() {
        with(state) {
            incrementScore(MissileCommandGameState.BONUS_SCORE_PER_LEVEL)

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

    private fun incrementScore(amount: Int) {
        state.score += amount
        client?.updateScore(state.score)
    }

    private fun fireEnemyMissile() {
        val aliveCities = state.cities.filter { it.health > 0 }

        if (aliveCities.isEmpty()) {
            // Don't really expect to get here, this is more defensive.
            // Elsewhere, we should be ending the game when the last city is dead. At this point,
            // we wont be able to target any cities anymore.
            return
        }

        val targetCity = aliveCities.random()

        val startX = (Math.random() * camera.viewportWidth).toFloat()
        val missile = EnemyMissile(state.missileSpeed, Vector2(startX, camera.viewportHeight), targetCity)

        state.enemyMissiles.add(missile)
        state.numMissilesRemaining --
    }

    private fun queueEnemyMissile() {
        val max = MissileCommandGameState.MAX_TIME_BETWEEN_ENEMY_MISSILES
        val min = MissileCommandGameState.MIN_TIME_BETWEEN_ENEMY_MISSILES
        state.nextEnemyMissileTime = state.timer + ((Math.random() * (max - min)) + min).toFloat()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
    }

}
