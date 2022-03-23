package com.serwylo.retrowars.games.spaceinvaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.input.SpaceInvadersSoftController
import kotlin.math.roundToInt

class SpaceInvadersGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.spaceInvaders,
    "Shoot the aliens",
    "Dodge their shots",
    400f,
    400f,
    true
) {

    private val state = SpaceInvadersGameState(viewport.worldWidth, viewport.worldHeight, game.uiAssets.getSprites().gameSprites.space_invaders_barrier)

    private val lifeContainer = HorizontalGroup().apply { space(UI_SPACE) }

    private val barrierTextures: MutableMap<Barrier, Texture> = state.barriers.associateWith { Texture(it.pixmap) }.toMutableMap()

    init {
        addGameScoreToHUD(lifeContainer)
    }

    override fun updateGame(delta: Float) {
        state.timer += delta

        controller!!.update(delta)

        if (getState() == State.Playing) {
            state.isMovingLeft = controller.trigger(SpaceInvadersSoftController.Buttons.LEFT)
            state.isMovingRight = controller.trigger(SpaceInvadersSoftController.Buttons.RIGHT)
            state.isFiring = controller.trigger(SpaceInvadersSoftController.Buttons.FIRE)

            if (state.nextPlayerRespawnTime <= 0) {
                updatePlayer(delta)
            }

            if (state.nextPlayerRespawnTime > 0f && state.timer > state.nextPlayerRespawnTime) {
                state.nextPlayerRespawnTime = -1f
                state.playerX = state.cellWidth + state.padding
            }
        }

        updateBullets(delta)

        if (state.nextLevelTime > 0) {
            if (state.timer > state.nextLevelTime) {
                state.level ++
                state.enemies = state.spawnEnemies()
                state.nextLevelTime = -1f
                state.timeUntilEnemyStep = SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP_SLOWEST
                state.timeUntilEnemyFire = SpaceInvadersGameState.INITIAL_DELAY_ENEMY_FIRE
                state.movingRow = state.enemies.size - 1
                state.enemyDirection = Direction.Right
                state.barriers = state.spawnBarriers(viewport.worldWidth)
                barrierTextures.clear()
                state.barriers.onEach { barrier -> barrierTextures[barrier] = Texture(barrier.pixmap) }
            }
        } else {

            if (isLowestRowAtTheBottom()) {
                if (getState() == State.Playing) {
                    endGame()
                }
            } else {
                maybeSpawnEnemyBullet(delta)
                moveEnemies(delta)
            }

        }

    }

    private fun isLowestRowAtTheBottom(): Boolean {
        val lowestRowY = state.enemies.lastOrNull { it.enemies.isNotEmpty() }?.y ?: 0f

        return lowestRowY < state.padding + state.cellHeight
    }

    private fun renderPlayer(r: ShapeRenderer) {
        r.begin(ShapeRenderer.ShapeType.Filled)
        r.color = Color.WHITE
        r.rect(
            state.playerX - state.cellWidth / 2,
            state.padding,
            state.cellWidth,
            state.cellHeight,
        )
        r.end()
    }

    override fun renderGame(camera: Camera) {

        val batch = SpriteBatch(1)
        batch.projectionMatrix = camera.combined
        batch.begin()
        barrierTextures.onEach { (barrier, texture) ->
            batch.draw(texture, barrier.x, barrier.y, barrier.width, barrier.height)
        }
        batch.end()

        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        if (state.nextPlayerRespawnTime <= 0) {
            renderPlayer(r)
        }

        r.begin(ShapeRenderer.ShapeType.Filled)
        r.color = Color.WHITE
        state.enemies.forEach { row ->
            row.enemies.forEach { enemy ->
                r.rect(
                    enemy.x,
                    row.y,
                    enemy.width,
                    state.cellHeight,
                )
            }
        }

        state.playerBullet?.also { renderBullet(r, it) }
        state.enemyBullets.forEach { renderBullet(r, it) }
        r.end()

        if (lifeContainer.children.size != state.numLives) {
            redrawLives()
        }
    }

    private fun redrawLives() {
        lifeContainer.clear()
        for (i in 0 until state.numLives) {
            lifeContainer.addActor(Label("x", game.uiAssets.getStyles().label.large))
        }
    }

    private fun renderBullet(r: ShapeRenderer, bullet: Bullet) {
        r.rect(
            bullet.x - state.bulletWidth / 2,
            bullet.y,
            state.bulletWidth,
            state.bulletHeight,
        )
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun onReceiveDamage(strength: Int) {

    }

    private fun countEnemies() = state.enemies.fold(0, { acc, row -> acc + row.enemies.size })

    private fun updateBullets(delta: Float) {
        state.playerBullet?.also { bullet ->
            bullet.y += state.playerBulletSpeed * delta

            if (bullet.y >= viewport.worldHeight) {
                state.playerBullet = null
            }

            if (checkPlayerBulletCollision(bullet)) {
                state.playerBullet = null
                if (countEnemies() == 0) {
                    state.nextLevelTime = state.timer + SpaceInvadersGameState.TIME_BETWEEN_LEVELS
                }
            }

            if (checkBulletBarrierCollision(bullet, false)) {
                state.playerBullet = null
            }
        }

        val it = state.enemyBullets.iterator()
        while (it.hasNext()) {
            val bullet = it.next()

            bullet.y -= state.enemyBulletSpeed * delta

            if (bullet.x - state.bulletWidth / 2 < state.playerX + state.cellWidth / 2 &&
                    bullet.x + state.bulletWidth / 2 > state.playerX - state.cellWidth / 2 &&
                    bullet.y + state.bulletHeight < state.padding + state.cellWidth) {

                onPlayerHit()
                it.remove()
            } else if (bullet.y < state.padding) {
                it.remove()
            } else if (checkBulletBarrierCollision(bullet, true)) {
                it.remove()
            }
        }
    }

    private fun checkBulletBarrierCollision(bullet: Bullet, isEnemyBullet: Boolean): Boolean {
        val barrier = state.barriers.firstOrNull { barrier ->
            bullet.y + state.bulletHeight > barrier.y
                && bullet.y < barrier.y + barrier.height
                && bullet.x > barrier.x
                && bullet.x < barrier.x + barrier.width
        } ?: return false

        // Offset the bullet position into the same reference frame as the barrier to make pixel collision detection easier.
        val bulletX = ((bullet.x - barrier.x) * barrier.pixmap.width / barrier.width).toInt()
        val bulletY = ((bullet.y - barrier.y) * barrier.pixmap.height / barrier.height).toInt()

        // Figure out the range of pixels in the Y dimension which overlap with the bullet:

        // Use roundToInt() instead of toInt() because a lot of the time multiplying by the
        // scaling factor "barrier.pixmap.height / barrier.height" may result in a value close to
        // the highest pixel in the barrier, but not exactly there. Truncating under these circumstances
        // will mean we never reach the end pixel.
        val bulletYIntersectEnd = (bulletY + state.bulletHeight * barrier.pixmap.height / barrier.height).roundToInt().coerceAtMost(barrier.pixmap.height)
        val bulletYIntersectStart = bulletY.coerceAtLeast(0)

        val pixelsToIterate = if (isEnemyBullet) bulletYIntersectEnd downTo bulletYIntersectStart else bulletYIntersectStart .. bulletYIntersectEnd

        for (y in pixelsToIterate) {
            // Use barrier.height - y because a value of 0 is at the *top* not the bottom like we expect.
            if (barrier.pixmap.getPixel(bulletX, (barrier.pixmap.height - y)) == 0xFFFFFFFF.toInt()) {

                applyBarrierDamage(
                    barrier,
                    if (isEnemyBullet) SpaceInvadersGameState.enemyBulletExplosion else SpaceInvadersGameState.playerBulletExplosion,
                    bulletX,
                    y,
                )
                return true
            }
        }

        return false
    }

    private fun applyBarrierDamage(barrier: Barrier, pattern: ExplosionPattern, hitX: Int, hitY: Int) {

        pattern.pattern.onEachIndexed { iy, row ->
            row.onEachIndexed { ix, cell ->
                if (cell) {
                    barrier.pixmap.drawPixel(
                        hitX - pattern.originX + ix,
                        barrier.pixmap.height - (hitY - pattern.originX + iy),
                        0x000000FF
                    )
                }
            }
        }

        barrierTextures[barrier]?.load(PixmapTextureData(barrier.pixmap, barrier.pixmap.format, false, false))
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

                // Intentionally treat the bullet as 1 dimensional, otherwise it is too hard to
                // have it travel in between columns of enemies as is often the case in the orignial.
                if (bullet.x > enemy.x && bullet.x < enemy.x + state.cellWidth) {
                    it.remove()

                    onEnemyHit(enemy)

                    return true
                }
            }
        }

        return false
    }

    private fun onEnemyHit(enemy: Enemy) {
        increaseScore(SpaceInvadersGameState.SCORE_PER_ENEMY)
    }

    private fun onPlayerHit() {
        state.numLives --

        if (state.numLives <= 0) {
            endGame()
        } else {
            state.nextPlayerRespawnTime = state.timer + SpaceInvadersGameState.PAUSE_AFTER_DEATH
        }
    }

    private fun maybeSpawnEnemyBullet(delta: Float) {

        if (state.enemyBullets.isNotEmpty()) {
            return
        }

        state.timeUntilEnemyFire -= delta

        if (state.timeUntilEnemyFire > 0) {
            return
        }

        state.timeUntilEnemyFire = SpaceInvadersGameState.DELAY_AFTER_ENEMY_FIRE

        val row = state.enemies.lastOrNull { it.enemies.isNotEmpty() }
        row?.enemies?.random()?.also {  toFire ->

            state.enemyBullets.add(
                Bullet(
                    x = toFire.x + state.cellWidth / 2,
                    y = row.y - state.bulletHeight,
                )
            )

        }

    }

    private fun moveEnemies(delta: Float) {

        state.timeUntilEnemyStep -= delta

        if (state.timeUntilEnemyStep > 0) {
            return
        }

        val enemiesLeft = (state.enemies.sumOf { it.enemies.size }).toFloat() / (SpaceInvadersGameState.NUM_ENEMIES_PER_ROW * SpaceInvadersGameState.NUM_ENEMY_ROWS)
        state.timeUntilEnemyStep = (SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP_SLOWEST - SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP_FASTEST) * enemiesLeft + SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP_FASTEST

        // Skip empty rows as the row we *were* moving may have been emptied by our bullets since
        // the previous step.
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