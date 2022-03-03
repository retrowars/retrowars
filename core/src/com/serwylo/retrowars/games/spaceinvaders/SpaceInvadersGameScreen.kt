package com.serwylo.retrowars.games.spaceinvaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.SpaceInvadersSoftController

class SpaceInvadersGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.spaceInvaders,
    "Shoot the aliens",
    "Dodge their shots",
    400f,
    400f,
    true
) {

    private val state = SpaceInvadersGameState(viewport.worldWidth, viewport.worldHeight)

    override fun updateGame(delta: Float) {
        state.timer += delta

        controller!!.update(delta)

        if (getState() == State.Playing) {
            state.isMovingLeft = controller.trigger(SpaceInvadersSoftController.Buttons.LEFT)
            state.isMovingRight = controller.trigger(SpaceInvadersSoftController.Buttons.RIGHT)
            state.isFiring = controller.trigger(SpaceInvadersSoftController.Buttons.FIRE)

            updatePlayer(delta)
        }

        updateBullets(delta)

        updateEnemies(delta)
    }

    override fun renderGame(camera: Camera) {
        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        r.begin(ShapeRenderer.ShapeType.Filled)
        r.color = Color.WHITE
        r.rect(
            state.playerX - state.cellWidth / 2,
            state.padding,
            state.cellWidth,
            state.cellHeight,
        )

        state.enemies.forEach { row ->
            row.enemies.forEach { enemy ->
                r.rect(
                    enemy.x,
                    row.y,
                    state.cellWidth,
                    state.cellHeight,
                )
            }
        }

        state.playerBullet?.also { bullet ->
            r.rect(
                bullet.x - state.bulletWidth / 2,
                bullet.y,
                state.bulletWidth,
                state.bulletHeight,
            )
        }
        r.end()
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun onReceiveDamage(strength: Int) {

    }

    private fun updateBullets(delta: Float) {
        state.playerBullet?.also { bullet ->
            bullet.y += SpaceInvadersGameState.PLAYER_BULLET_SPEED * delta

            if (bullet.y >= viewport.worldHeight) {
                state.playerBullet = null
            }

            if (checkPlayerBulletCollision(bullet)) {
                state.playerBullet = null
            }
        }
    }

    private fun checkPlayerBulletCollision(bullet: Bullet): Boolean {
        val collisionRows = state.enemies.filter { row ->
            bullet.y + state.bulletHeight > row.y &&
                    bullet.y < row.y + state.cellHeight
        }

        for (row in collisionRows) {
            val it = row.enemies.iterator()
            while (it.hasNext()) {
                val enemy = it.next()

                if (enemy.x + state.cellWidth > bullet.x - state.bulletWidth &&
                    enemy.x < bullet.x + state.bulletWidth) {

                    it.remove()
                    return true
                }
            }
        }

        return false
    }

    private fun updateEnemies(delta: Float) {

        state.timeUntilEnemyStep -= delta

        if (state.timeUntilEnemyStep > 0) {
            return
        }

        state.timeUntilEnemyStep = SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP

        // Skip empty rows
        while (state.movingRow >= 0 && state.enemies[state.movingRow].enemies.isEmpty()) {
            state.movingRow --
        }

        if (state.movingRow == -1) {

            if (!shouldEnemiesDrop()) {
                state.movingRow = state.enemies.indexOfLast { it.enemies.isNotEmpty() }
            } else {
                dropEnemyRow()

                // Return rather than continuing to shuffle along in the same time step.
                return
            }

        }

        val row = state.enemies.getOrNull(state.movingRow)
        if (row != null) {
            shuffleEnemyRowAcross(row)
        }

        state.movingRow --

    }

    private fun shouldEnemiesDrop() = state.enemies.any { row ->
        if (state.enemyDirection == Direction.Right) {
            val x = row.enemies.lastOrNull()?.x ?: Float.MIN_VALUE
            x + state.cellWidth + state.padding > viewport.worldWidth - state.padding
        } else {
            val x = row.enemies.firstOrNull()?.x ?: Float.MAX_VALUE
            x - state.padding < state.padding
        }
    }

    private fun shuffleEnemyRowAcross(row: EnemyRow) {
        row.enemies.forEach { enemy ->
            if (state.enemyDirection == Direction.Right) {
                enemy.x += state.enemyStepSize
            } else {
                enemy.x -= state.enemyStepSize
            }
        }
    }

    private fun dropEnemyRow() {
        state.enemies.forEach { row ->
            row.y -= state.cellHeight + state.padding
        }

        state.enemyDirection = if (state.enemyDirection == Direction.Right) Direction.Left else Direction.Right
    }

    private fun updatePlayer(delta: Float) {

        if (state.isFiring && state.playerBullet == null) {
            state.playerBullet = Bullet(state.playerX, state.padding + state.cellHeight)
            state.isFiring = false
        }

        val distance = SpaceInvadersGameState.PLAYER_SPEED * delta

        if (state.isMovingLeft) {
            if (state.playerX - distance > state.padding + state.cellWidth / 2) {
                state.playerX -= distance
            }
        }

        if (state.isMovingRight) {
            if (state.playerX + distance < viewport.worldWidth - state.padding - state.cellWidth / 2) {
                state.playerX += distance
            }
        }
    }

}