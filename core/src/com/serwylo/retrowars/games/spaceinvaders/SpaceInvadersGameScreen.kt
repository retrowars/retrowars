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
        }

        updatePlayer(delta)
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

        state.enemies.forEach { enemyRow ->
            enemyRow.forEach { enemy ->
                r.rect(
                    enemy.x,
                    enemy.y,
                    state.cellWidth,
                    state.cellHeight,
                )
            }
        }
        r.end()
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun onReceiveDamage(strength: Int) {

    }

    private fun updateEnemies(delta: Float) {

        state.timeUntilEnemyStep -= delta

        if (state.timeUntilEnemyStep > 0) {
            return
        }

        state.timeUntilEnemyStep = SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP

        // Skip empty rows
        while (state.movingRow >= 0 && state.enemies[state.movingRow].isEmpty()) {
            state.movingRow --
        }

        if (state.movingRow == -1) {

            if (!shouldEnemiesDrop()) {
                state.movingRow = state.enemies.indexOfLast { it.isNotEmpty() }
            } else {
                dropEnemyRow()

                // Return rather than continuing to shuffle along in the same time step.
                return
            }

        }

        shuffleEnemyRowAcross()
        state.movingRow --

    }

    private fun shouldEnemiesDrop() = state.enemies.any { row ->
        if (state.enemyDirection == Direction.Right) {
            val x = row.lastOrNull()?.x ?: Float.MIN_VALUE
            x + state.cellWidth + state.padding > viewport.worldWidth - state.padding
        } else {
            val x = row.firstOrNull()?.x ?: Float.MAX_VALUE
            x - state.padding < state.padding
        }
    }

    private fun shuffleEnemyRowAcross() {
        state.enemies.getOrNull(state.movingRow)?.forEach { enemy ->
            if (state.enemyDirection == Direction.Right) {
                enemy.x += state.enemyStepSize
            } else {
                enemy.x -= state.enemyStepSize
            }
        }
    }

    private fun dropEnemyRow() {
        state.enemies.forEach { row ->
            row.forEach { e ->
                e.y -= state.cellHeight + state.padding
            }
        }

        state.enemyDirection = if (state.enemyDirection == Direction.Right) Direction.Left else Direction.Right
    }

    private fun updatePlayer(delta: Float) {
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