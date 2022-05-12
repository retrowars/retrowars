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
import com.serwylo.retrowars.games.asteroids.AsteroidsSoundLibrary
import com.serwylo.retrowars.input.SpaceInvadersSoftController
import kotlin.math.roundToInt

class SpaceInvadersGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.spaceInvaders,
    400f,
    400f,
    true
) {

    private val state = SpaceInvadersGameState(viewport.worldWidth, viewport.worldHeight, game.uiAssets.getSprites().gameSprites.space_invaders_barrier)

    private val lifeContainer = HorizontalGroup().apply { space(UI_SPACE) }

    private val sounds = SpaceInvadersSoundLibrary()

    private val barrierTextures: MutableMap<Barrier, Texture> = state.barriers.associateWith { Texture(it.pixmap) }.toMutableMap()

    init {
        addGameScoreToHUD(lifeContainer)
        sounds.tick()
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
        val lowestRowY = state.enemies.lastOrNull { it.isNotEmpty() }?.y ?: 0f

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
        state.enemies.forEach { row ->
            row.cells.forEach { cell ->
                if (cell.hasEnemy) {
                    r.color = if (state.networkEnemyCells.contains(cell)) Color.RED else Color.WHITE
                    r.rect(
                     cell.x + (state.cellWidth - row.enemyWidth) / 2,
                        row.y,
                        row.enemyWidth,
                        state.cellHeight,
                    )
                }
            }
        }

        r.color = Color.WHITE
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
        for (i in 0 until strength) {
            for (j in 0 until 4) {
                spawnNetworkEnemy()
            }
        }
    }

    /**
     * Try to spawn in the lowest row with any enemies, unless that is a little bit too low,
     * in which case we set a minimum height near the barriers.
     *
     * Obviously can only spawn in rows with empty cells. If the lowest eligible row doesn't have
     * empty cells, move up.
     *
     * Prefer to spawn as horizontally close to pre-existing enemies as possible (i.e. fill in the
     * gaps before moving further away from the edges).
     *
     * If there are no eligible rows, then spawn a new one above the top row. If it goes off the top
     * of the screen then that is fine, we'll just keep respawning them higher and higher off the
     * screen.
     */
    private fun spawnNetworkEnemy() {

        // Prefer to spawn within the bounds of what aliens are left on the screen.
        // Otherwise, for example, if there is only one column remaining, we don't want to spawn
        // the next enemy way off in the distance in a place that is potentially far outside
        // the bounds of the screen. Technically it will work (the row will drop, turn around, and
        // continue moving until the enemy ends up on screen), but it is unintuitive, because the
        // player can't see the newly spawned enemy and may think the game is buggy.
        val minX = state.enemies
            .mapNotNull { row -> row.cells.firstOrNull { it.hasEnemy }?.x }
            .minOrNull() ?: state.padding

        val maxX = state.enemies
            .mapNotNull { row -> row.cells.lastOrNull { it.hasEnemy }?.x }
            .maxOrNull() ?: viewport.worldWidth - state.padding - state.cellWidth

        // This is approximately in the middle of a barrier - don't spawn into rows below this or
        // the player may die almost instantly without any opportunity to defend themselves.
        val minSpawnHeight = state.padding * 3 + state.cellHeight * 3
        val rowsAboveThreshold = state.enemies.filter { it.y > minSpawnHeight }
        val lastRowWithEmptySpace = rowsAboveThreshold.lastOrNull { row -> row.cells.any { !it.hasEnemy } }

        // No space available in any of the existing enemy rows, so lets spawn a new row.
        // If we're spawning a new row, then almost by definition the main alien grid has dropped
        // low enough that we know any new grids will have small aliens in them.
        val rowToSpawnEnemy: EnemyRow = if (lastRowWithEmptySpace == null) {
            val firstRow = state.enemies.first()
            val newRow = state.spawnEnemyRow(
                y = firstRow.y + state.padding + state.cellHeight,
                startX = firstRow.cells.first().x,
                enemyWidth = state.cellWidth * SpaceInvadersGameState.ROW_WIDTHS.first(),
                hasEnemies = false,
            )

            state.enemies = listOf(newRow) + state.enemies
            state.movingRow ++

            newRow
        } else {
            lastRowWithEmptySpace
        }

        val preferredCells = state.enemies
            .filter { it.y > minSpawnHeight }
            .map { row -> row.cells.filter { cell -> !cell.hasEnemy && cell.x >= minX && cell.x <= maxX } }
            .lastOrNull { it.isNotEmpty() } ?: emptyList()

        val cell = if (preferredCells.isNotEmpty()) {
            preferredCells.random()
        } else {

            // If we can't find a preferred cell within the bounds of existing enemies on screen,
            // then pick a cell that is closest to the existing cohort of aliens to minimize the
            // change of appearing well off screen.
            rowToSpawnEnemy.cells
                .filter { !it.hasEnemy }
                .minByOrNull { cell ->
                    when {
                        cell.x <= minX -> minX - cell.x
                        cell.x >= maxX -> cell.x - maxX
                        else -> Float.MAX_VALUE
                    }
                } ?: return // Should never be null, but safely stop if it is.
        }

        cell.hasEnemy = true
        state.networkEnemyCells.add(cell)

    }

    private fun countEnemies() = state.enemies.fold(0) { acc, row -> acc + row.cells.count { it.hasEnemy } }

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
            for (x in row.cells.indices) {
                val cell = row.cells[x]

                if (!cell.hasEnemy) {
                    continue
                }

                // Intentionally treat the bullet as 1 dimensional, otherwise it is too hard to
                // have it travel in between columns of enemies as is often the case in the original.
                if (bullet.x > cell.x + (state.cellWidth - row.enemyWidth) / 2 && bullet.x < cell.x + (state.cellWidth - row.enemyWidth) / 2 + row.enemyWidth) {
                    cell.hasEnemy = false
                    // state.networkEnemyCells.remove(cell)

                    onEnemyHit()

                    return true
                }
            }
        }

        return false
    }

    private fun onEnemyHit() {
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

        val bottomMostCells: List<Pair<EnemyRow, EnemyCell>> = (0 until state.enemies.first().cells.size).map { i ->
            val row = state.enemies.lastOrNull { it.cells[i].hasEnemy }

            if (row == null) {
                null
            } else {
                row to row.cells[i]
            }
        }.filterNotNull()

        if (bottomMostCells.isEmpty()) {
            return
        }

        bottomMostCells.random().also { (row, cell) ->

            state.enemyBullets.add(
                Bullet(
                    x = cell.x + state.cellWidth / 2,
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

        val enemiesLeft = (state.enemies.sumOf { row -> row.countEnemies() }).toFloat() / (SpaceInvadersGameState.NUM_ENEMIES_PER_ROW * SpaceInvadersGameState.NUM_ENEMY_ROWS)
        val slowest = SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP_SLOWEST
        val fastest = SpaceInvadersGameState.TIME_BETWEEN_ENEMY_STEP_FASTEST
        state.timeUntilEnemyStep = ((slowest - fastest) * enemiesLeft + fastest).coerceIn(fastest, slowest)

        // Skip empty rows as the row we *were* moving may have been emptied by our bullets since
        // the previous step.
        while (state.movingRow >= 0 && state.enemies[state.movingRow].cells.all { !it.hasEnemy }) {
            state.movingRow --
        }

        if (state.movingRow == -1) {

            sounds.tick()

            if (!shouldEnemiesDrop()) {
                state.movingRow = state.enemies.indexOfLast { it.isNotEmpty() }
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
            val x = row.cells.lastOrNull { it.hasEnemy }?.x ?: Float.MIN_VALUE
            x + state.cellWidth + state.padding > viewport.worldWidth - state.padding
        } else {
            val x = row.cells.firstOrNull { it.hasEnemy }?.x ?: Float.MAX_VALUE
            x - state.padding < state.padding
        }
    }

    private fun shuffleEnemyRowAcross(row: EnemyRow) {
        row.cells.forEach { cell ->
            if (state.enemyDirection == Direction.Right) {
                cell.x += state.enemyStepSize
            } else {
                cell.x -= state.enemyStepSize
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