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
import com.serwylo.retrowars.games.missilecommand.entities.Explosion
import com.serwylo.retrowars.games.missilecommand.entities.Missile
import com.serwylo.retrowars.games.missilecommand.entities.Turret
import kotlin.math.abs

class MissileCommandGameScreen(private val game: RetrowarsGame) : Screen {

    companion object {
        const val MIN_WORLD_WIDTH = 400f
        const val MIN_WORLD_HEIGHT = 400f
        const val TAG = "MissileCommandGameScreen"
    }

    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, camera)

    private val state: MissileCommandGameState

    private val hud: HUD

    init {
        viewport.apply(true)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        state = MissileCommandGameState(viewport.worldWidth, viewport.worldHeight)

        hud = HUD(state, game.uiAssets)
    }

    override fun show() {
        Gdx.input.inputProcessor = object: InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val worldPos = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
                val closest = state.turrets.minBy { abs(it.position.x - worldPos.x) }
                fire(closest!!, Vector2(worldPos.x, worldPos.y))
                return true
            }
        }
    }

    private fun fire(turret: Turret, target: Vector2) {
        state.friendlyMissiles.add(Missile(turret.position, target))
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

        state.enemyMissiles.forEach { it.update(delta) }

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
