package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.tetris.ButtonState
import com.serwylo.retrowars.input.TempestSoftController
import kotlin.random.Random

class TempestGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.tempest,
    "Shoot the enemies",
    "Don't let them touch you",
    40f,
    40f,
    isOrthographic = false,
) {

    companion object {
        @Suppress("unused")
        const val TAG = "TempestGameScreen"
    }

    private val state = TempestGameState(viewport.worldWidth, viewport.worldHeight)

    private val lifeContainer = HorizontalGroup().apply { space(UI_SPACE) }

    init {
        queueEnemy()
        spawnEnemy()

        controller!!.listen(
            TempestSoftController.Buttons.MOVE_COUNTER_CLOCKWISE,
            { state.moveCounterClockwise = if (state.moveCounterClockwise == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held },
            { state.moveCounterClockwise = ButtonState.Unpressed })

        controller!!.listen(
            TempestSoftController.Buttons.MOVE_CLOCKWISE,
            { state.moveClockwise = if (state.moveClockwise == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held },
            { state.moveClockwise = ButtonState.Unpressed })

        controller!!.listen(
            TempestSoftController.Buttons.FIRE,
            { state.fire = if (state.fire == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held },
            { state.fire = ButtonState.Unpressed })

        addGameScoreToHUD(lifeContainer)
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
        state.timer += delta

        if (getState() == State.Playing) {
            if (state.nextPlayerRespawnTime > 0) {
                maybeRespawnPlayer()
            } else {

                recordInput()
                movePlayer()

                if (state.nextLevelTime > 0) {
                    maybeAdvanceLevel()
                } else {
                    fire()
                    checkIfPlayerHit()
                    checkEndLevel()
                    if (state.numLives <= 0) {
                        endGame()
                    }
                }

                resetInput()
            }
        }

        moveBullets(delta)
        moveEnemies(delta)
        checkCollisions(delta)
        maybeSpawnEnemies()
        updateExplosions()
    }

    private fun checkCollisions(delta: Float) {

        val hitEnemies = mutableListOf<Enemy>()

        val enemyIt = state.enemies.iterator()
        while (enemyIt.hasNext()) {
            val enemy = enemyIt.next()
            val apparentSegment = apparentSegment(enemy)

            val bulletIt = state.bullets.iterator()
            while (bulletIt.hasNext()) {
                val bullet = bulletIt.next()

                /* Continuous collision detection, as our fast moving bullets will often pass through
                 * enemies for any given frame:
                 *
                 *   t1: [B] ->   <- [E]
                 *   t2:     <- [E] [B] ->
                 *
                 * For each bullet-enemy pair (on the same segment), simulate the movement of each
                 * without actually moving them, then check if the bullet passed through.
                 */
                if (apparentSegment === bullet.segment &&

                    // Only include bullets yet to pass beyond the deepest part of the enemy, no
                    // need to even simulate any movement for these, they will not hit.
                    bullet.zPosition < enemy.zPosition + enemy.depth

                    && (
                        // Either they are overlapping right now, before any simulated movement...
                        bullet.zPosition > enemy.zPosition
                        ||
                        // ... or once we advance the bullet and the enemy toward each other, the
                        // bullet is now either overlapping or beyond the enemy.
                        bullet.zPosition + TempestGameState.BULLET_SPEED * delta > enemy.zPosition + state.enemySpeed * delta
                    )

                ) {
                    increaseScore(TempestGameState.SCORE_PER_ENEMY)
                    queueExplosion(exactEnemyPosition(enemy), enemy.zPosition)

                    bulletIt.remove()
                    enemyIt.remove()

                    hitEnemies.add(enemy)
                }

            }
        }

        for (enemy in hitEnemies) {
            if (enemy is FlipperTanker) {
                spawnFlippersFromTanker(enemy)
            }
        }

    }

    private fun spawnFlippersFromTanker(enemy: FlipperTanker) {
        state.enemies.add(Flipper(enemy.segment.next(Direction.Clockwise), enemy.zPosition, state.enemyFlipWaitTime))
        state.enemies.add(Flipper(enemy.segment.next(Direction.CounterClockwise), enemy.zPosition, state.enemyFlipWaitTime))
    }

    private fun updateExplosions() {
        state.explosions.removeAll { state.timer > it.startTime + TempestGameState.EXPLOSION_TIME }
    }

    private fun checkEndLevel() {
        if (getState() == State.Playing && state.numEnemiesRemaining == 0 && state.enemies.isEmpty()) {
            state.nextLevelTime = state.timer + TempestGameState.TIME_BETWEEN_LEVELS
        }
    }

    private fun maybeAdvanceLevel() {
        if (state.timer > state.nextLevelTime) {
            state.bullets.clear()
            state.level = state.allLevels.filter { it != state.level }.random()
            state.playerSegment = state.level.segments[0]
            state.levelCount ++
            state.numEnemiesRemaining = TempestGameState.BASE_ENEMIES_PER_LEVEL + (state.levelCount * TempestGameState.ADDITIONAL_ENEMIES_PER_LEVEL)
            state.nextLevelTime = 0f
            state.increaseSpeed()
        }
    }

    private fun maybeRespawnPlayer() {
        if (state.timer > state.nextPlayerRespawnTime) {
            state.bullets.clear()
            state.enemies.clear()
            state.playerSegment = state.level.segments[0]
            state.numEnemiesRemaining = TempestGameState.BASE_ENEMIES_PER_LEVEL + state.levelCount
            state.nextEnemyTime = state.timer + TempestGameState.PAUSE_AFTER_DEATH
            state.nextPlayerRespawnTime = 0f
        }
    }

    private fun maybeSpawnEnemies() {
        if (state.numEnemiesRemaining > 0 && state.shouldSpawnEnemy()) {
            spawnEnemy()
            queueEnemy()
        }
    }

    private fun spawnEnemy() {
        val nextFlipTime = state.enemyFlipTransitionTime + state.enemyFlipWaitTime - TempestGameState.TIME_BETWEEN_ENEMIES_VARIATION + Random.nextFloat() * TempestGameState.TIME_BETWEEN_ENEMIES_VARIATION
        state.enemies.add(makeEnemy(state.level.segments.random(), nextFlipTime))
        state.numEnemiesRemaining --
    }

    private fun recordInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            state.moveCounterClockwise = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            state.moveClockwise = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            state.fire = ButtonState.JustPressed
        }
    }

    private fun resetInput() {
        state.moveCounterClockwise = ButtonState.Unpressed
        state.moveClockwise = ButtonState.Unpressed
        state.fire = ButtonState.Unpressed
    }

    private fun moveEnemies(delta: Float) {
        val enemyIt = state.enemies.iterator()
        val toRemove = mutableListOf<Enemy>()
        while (enemyIt.hasNext()) {
            val enemy = enemyIt.next()
            val remove = when (enemy) {
                is Flipper -> moveFlipper(delta, enemy)
                is FlipperTanker -> moveFlipperTanker(delta, enemy)
            }

            if (remove) {
                toRemove.add(enemy)
                enemyIt.remove()
            }
        }

        for (enemy in toRemove) {
            if (enemy is FlipperTanker) {
                spawnFlippersFromTanker(enemy)
            }
        }
    }

    /**
     * Enemies that flip from one segment to another (e.g. [Flipper]s) will store their current
     * [Segment] up until the very last frame of a particular flip sequence. This allows for proper
     * animation from the source to destination segment.
     *
     * HOWEVER, for the purposes of collision detection with bullets and the player, we have this
     * notion that once they are past half way flipping from one to another, we should consider their
     * location to be in the next segment instead.
     */
    private fun apparentSegment(enemy: Enemy): Segment = when(enemy) {
        is Flipper ->
            // Either not flipping, or flipping but hasn't passed half way between segments yet, so just look at its normal segment.
            if (enemy.timeUntilNextFlip > state.enemyFlipTransitionTime / 2) enemy.segment

            // Started flipping and past half way, so consider it in its adjacent segment.
            else enemy.segment.next(enemy.direction)

        else -> enemy.segment
    }

    private fun moveFlipper(delta: Float, enemy: Flipper): Boolean {
        enemy.timeUntilNextFlip -= delta

        if (enemy.timeUntilNextFlip < 0) {
            enemy.segment = enemy.segment.next(enemy.direction)
            enemy.timeUntilNextFlip = state.enemyFlipWaitTime + state.enemyFlipTransitionTime

            if (enemy.zPosition > 0) {
                // Before getting to the end of the screen, enemies sometimes flip, and
                // sometimes don't. If they do, they seem to for an arbitrary period before
                // stopping and then beginning flipping again in the future.
                val waitBeforeFlippingAgain = Random.nextFloat() > 0.3f
                if (waitBeforeFlippingAgain) {
                    enemy.timeUntilNextFlip = state.enemyFlipTransitionTime + state.enemyFlipWaitTime - TempestGameState.ENEMY_FLIP_WAIT_TIME_VARIATION + Random.nextFloat() * TempestGameState.ENEMY_FLIP_WAIT_TIME_VARIATION
                }

                val changeDirection = Random.nextFloat() > 0.8f
                if (changeDirection) {
                    enemy.direction = oppositeDirection(enemy.direction)
                }
            }
        }

        if (enemy.zPosition > 0) {
            // March enemies forward toward the screen...
            enemy.zPosition -= state.enemySpeed * delta

            // ... when they have walked to the end of the screen, tell them they are now flipping.
            if (enemy.zPosition <= 0) {
                enemy.zPosition = 0f

                // If we were in the middle of waiting some time before flipping, ignore that and
                // queue up the next flip at the approved interval.
                enemy.timeUntilNextFlip = enemy.timeUntilNextFlip.coerceAtMost(state.enemyFlipWaitTime + state.enemyFlipTransitionTime)
            }
        }

        return false
    }

    private fun moveFlipperTanker(delta: Float, enemy: FlipperTanker): Boolean {
        enemy.zPosition -= state.enemySpeed * delta

        return enemy.zPosition <= 0
    }

    private fun queueExplosion(position: Vector2, depth: Float) {
        // March enemies forward toward the screen...
        state.explosions.add(Explosion(Vector3(position.x, position.y, depth), state.timer))
    }

    private fun checkIfPlayerHit() {
        val enemies = state.enemies.filter {
            it.zPosition <= 0 && it.segment == state.playerSegment
        }

        if (enemies.isNotEmpty()) {
            state.numLives --
            queueExplosion(state.playerSegment.centre, enemies[0].zPosition)
            queueExplosion(state.playerSegment.centre.cpy().add(1.5f, 1.5f), enemies[0].zPosition)
            queueExplosion(state.playerSegment.centre.cpy().add(-1.5f, -1.5f), enemies[0].zPosition)
            queueExplosion(state.playerSegment.centre.cpy().add(-1.5f, 1.5f), enemies[0].zPosition)
            queueExplosion(state.playerSegment.centre.cpy().add(1.5f, -1.5f), enemies[0].zPosition)

            state.nextPlayerRespawnTime = state.timer + TempestGameState.PAUSE_AFTER_DEATH
        }
    }

    private fun moveBullets(delta: Float) {
        val bulletIt = state.bullets.iterator()
        while (bulletIt.hasNext()) {
            val bullet = bulletIt.next()
            bullet.zPosition += TempestGameState.BULLET_SPEED * delta
            if (bullet.zPosition > TempestGameState.LEVEL_DEPTH) {
                bulletIt.remove()
            }
        }
    }

    private fun movePlayer() {
        val currentIndex = state.level.segments.indexOf(state.playerSegment)

        if (state.moveClockwise == ButtonState.JustPressed) {
            state.playerSegment = state.level.segments[(state.level.segments.size + currentIndex - 1) % state.level.segments.size]
        } else if (state.moveCounterClockwise == ButtonState.JustPressed) {
            state.playerSegment = state.level.segments[(currentIndex + 1) % state.level.segments.size]
        }
    }

    private fun fire() {
        if (state.fire != ButtonState.JustPressed) {
            return
        }

        val flippersToHit = mutableListOf<Flipper>()
        for (enemy in state.enemies) {
            if (enemy is Flipper && enemy.zPosition <= 0 && apparentSegment(enemy) === state.playerSegment) {
                flippersToHit.add(enemy)
            }
        }
        flippersToHit.sortBy { it.timeUntilNextFlip }

        if (flippersToHit.isNotEmpty()) {
            // Just kill it directly without even showing the bullet. We are right on top of this enemy.
            state.enemies.remove(flippersToHit[0])
            increaseScore(TempestGameState.SCORE_PER_ENEMY)
            queueExplosion(exactEnemyPosition(flippersToHit[0]), 0f)
        } else {
            state.bullets.add(Bullet(state.playerSegment, 0f))
        }
    }

    override fun onReceiveDamage(strength: Int) {
    }

    override fun renderGame(camera: Camera) {
        camera.apply {
            position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f - viewport.worldHeight / 8f, viewport.worldHeight.coerceAtMost(viewport.worldWidth))
            lookAt(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
            update()
        }

        renderLevel(camera)

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

    private fun renderLevel(camera: Camera) {
        val r = game.uiAssets.shapeRenderer

        r.begin(ShapeRenderer.ShapeType.Line)
        r.projectionMatrix = camera.combined

        r.color = Color.BLUE
        state.level.segments.forEach { segment ->
            renderSegment(r, segment)
        }

        if (state.nextPlayerRespawnTime <= 0f) {
            r.color = Color.YELLOW
            renderSegment(r, state.playerSegment)
            renderPlayer(r)
        }

        state.bullets.onEach {
            renderBullet(r, it)
        }

        r.color = Color.WHITE
        state.enemies.onEach {
            renderEnemy(r, it)
        }

        r.color = Color.YELLOW
        state.explosions.onEach {
            renderExplosion(r, it)
        }

        r.end()
    }

    private val playerShape = arrayOf(
        0f, -1.2f,
        2f, 0f,
        0.5f, 4f,
        1.2f, 0.7f,

        0f, 0.2f,

        -1.2f, 0.7f,
        -0.5f, 4f,
        -2f, 0f,
    ).toFloatArray()

    private val flipperShape = arrayOf(
        2.2f, 0f,
        1.7f, 1.3f,
        2.2f, 2.6f,

        -2.2f, 0f,
        -1.7f, 1.3f,
        -2.2f, 2.6f,
    ).toFloatArray()

    private fun renderOnAngle(shapeRenderer: ShapeRenderer, pos: Vector2, depth: Float, angleInDegrees: Float, block: (shapeRenderer: ShapeRenderer) -> Unit) {
        shapeRenderer.translate(pos.x, pos.y, depth)
        shapeRenderer.rotate(0f, 0f, 1f, angleInDegrees)
        shapeRenderer.rotate(0f, 1f, 0f, 180f)
        shapeRenderer.rotate(1f, 0f, 0f, 90f)
        block(shapeRenderer)
        shapeRenderer.identity()
    }

    private fun renderPlayer(shapeRenderer: ShapeRenderer) {
        renderOnAngle(shapeRenderer, state.playerSegment.centre, 0f, state.playerSegment.angle) {
            it.polygon(playerShape)
        }
    }

    private fun renderExplosion(shapeRenderer: ShapeRenderer, explosion: Explosion) {
        shapeRenderer.translate(explosion.position.x, explosion.position.y, -explosion.position.z)

        val progress = (state.timer - explosion.startTime) / TempestGameState.EXPLOSION_TIME

        val random = Random(explosion.hashCode())
        for (i in 0..10) {
            val point = Vector3(random.nextFloat() * 5 - 2.5f, random.nextFloat() * 5 - 2.5f, random.nextFloat() * 5 - 2.5f)
            shapeRenderer.line(Vector3(), Vector3(point.scl(progress)))
        }

        shapeRenderer.identity()
    }

    private fun queueEnemy() {
        val max = state.timeBetweenEnemies + TempestGameState.TIME_BETWEEN_ENEMIES_VARIATION / 2f
        val min = max - TempestGameState.TIME_BETWEEN_ENEMIES_VARIATION
        state.nextEnemyTime = state.timer + ((Math.random() * (max - min)) + min).toFloat()
    }

    private fun renderEnemy(shapeRenderer: ShapeRenderer, enemy: Enemy) = when (enemy) {
        is Flipper -> renderFlipper(shapeRenderer, enemy)
        is FlipperTanker -> renderFlipperTanker(shapeRenderer, enemy)
    }

    private fun renderFlipper(shapeRenderer: ShapeRenderer, enemy: Flipper) {
        if (enemy.timeUntilNextFlip > state.enemyFlipTransitionTime /* Not yet moving to the next segment */) {
            renderOnAngle(shapeRenderer, enemy.segment.centre, -enemy.zPosition, enemy.segment.angle) {
                it.polygon(flipperShape)
            }
        } else {
            val flipPercent = 1f - enemy.timeUntilNextFlip / state.enemyFlipTransitionTime
            val nextSegment = enemy.segment.next(enemy.direction)

            val pos = enemy.segment.centre.cpy().add(nextSegment.centre.cpy().sub(enemy.segment.centre).scl(flipPercent))
            val angle = enemy.segment.angle + (flipPercent * if (enemy.direction == Direction.Clockwise) -180f else 180f)

            renderOnAngle(shapeRenderer, pos, -enemy.zPosition, angle) {
                it.polygon(flipperShape)
            }
        }
    }

    private fun renderFlipperTanker(shapeRenderer: ShapeRenderer, enemy: FlipperTanker) {
        renderOnAngle(shapeRenderer, enemy.segment.centre, -enemy.zPosition, enemy.segment.angle) {
            it.rotate(1f, 1f, 1f, 45f)
            it.box(-1f, -1f, -1f, 2f, 2f, 2f)
        }
    }

    private fun exactEnemyPosition(enemy: Enemy): Vector2 = when(enemy) {
        is Flipper -> exactFlipperPosition(enemy)
        else -> enemy.segment.centre
    }

    private fun exactFlipperPosition(enemy: Flipper): Vector2 {
        if (enemy.timeUntilNextFlip > state.enemyFlipTransitionTime /* Not yet moving to the next segment */) {
            return enemy.segment.centre
        } else {
            val flipPercent = 1f - enemy.timeUntilNextFlip / state.enemyFlipTransitionTime
            val nextSegment = enemy.segment.next(enemy.direction)
            return enemy.segment.centre.cpy().add(nextSegment.centre.cpy().sub(enemy.segment.centre).scl(flipPercent))
        }
    }

    private fun renderBullet(shapeRenderer: ShapeRenderer, bullet: Bullet) {
        shapeRenderer.box(bullet.segment.centre.x, bullet.segment.centre.y, -bullet.zPosition, 1f, 1f, 1f)
    }

    private fun renderSegment(shapeRenderer: ShapeRenderer, segment: Segment) {
        shapeRenderer.line(segment.start.x, segment.start.y, -TempestGameState.LEVEL_DEPTH, segment.end.x, segment.end.y, -TempestGameState.LEVEL_DEPTH)
        shapeRenderer.line(segment.start.x, segment.start.y, 0f, segment.start.x, segment.start.y, -TempestGameState.LEVEL_DEPTH)
        shapeRenderer.line(segment.end.x, segment.end.y, 0f, segment.end.x, segment.end.y, -TempestGameState.LEVEL_DEPTH)
        shapeRenderer.line(segment.start, segment.end)
    }

}
