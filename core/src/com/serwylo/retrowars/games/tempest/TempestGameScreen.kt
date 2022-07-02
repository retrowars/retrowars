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
import com.serwylo.retrowars.input.TempestSoftController
import com.serwylo.retrowars.ui.ENEMY_ATTACK_COLOUR
import java.lang.IllegalStateException
import java.util.*
import kotlin.random.Random

class TempestGameScreen(game: RetrowarsGame) : GameScreen(
    game,
    Games.tempest,
    40f,
    40f,
    isOrthographic = false,
) {

    companion object {
        @Suppress("unused")
        const val TAG = "TempestGameScreen"
    }

    private val state = TempestGameState(viewport.worldWidth, viewport.worldHeight)
    private val sounds = TempestSoundLibrary()
    override fun getSoundLibrary() = sounds

    private val lifeContainer = HorizontalGroup().apply { space(UI_SPACE) }

    init {
        addGameScoreToHUD(lifeContainer)

        initEnemyPool()
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

                controller?.update(delta)
                movePlayer()
                fire()

                if (state.nextLevelTime > 0) {
                    if (advancePlayer()) {
                        maybeAdvanceLevel()
                    }
                } else {
                    checkEndLevel()
                }

                if (state.numLives <= 0) {
                    endGame()
                }
            }
        }

        maybeSpawnEnemy(delta)
        moveBullets(delta)
        moveEnemies(delta)
        checkCollisions(delta)
        updateExplosions()
    }

    private fun maybeSpawnEnemy(delta: Float) {
        state.nextEnemyTime -= delta
        if (state.nextEnemyTime < 0) {
            if (state.poolOfFlipperTankers.isNotEmpty() && state.numSpawnedFromPoolFlipperTankers < 2) {
                state.numSpawnedFromPoolFlipperTankers ++
                maybeSpawnFromPool(state.poolOfFlipperTankers)
            }

            if (state.poolOfFlippers.isNotEmpty() && state.numSpawnedFromPoolFlippers < 4) {
                state.numSpawnedFromPoolFlippers ++
                maybeSpawnFromPool(state.poolOfFlippers)
            }

            if (state.poolOfSpikeBuilders.isNotEmpty() && state.numSpawnedFromPoolSpikeBuilder < 3) {
                state.numSpawnedFromPoolSpikeBuilder ++
                maybeSpawnFromPool(state.poolOfSpikeBuilders)?.let {
                    state.enemies += it.spike
                }
            }

            val max = state.timeBetweenEnemies + TempestGameState.TIME_BETWEEN_ENEMIES_VARIATION / 2f
            val min = max - TempestGameState.TIME_BETWEEN_ENEMIES_VARIATION
            state.nextEnemyTime = ((Math.random() * (max - min)) + min).toFloat()
        }
    }

    private fun checkCollisions(delta: Float) {

        val hitEnemies = mutableListOf<Enemy>()

        val bulletIt = state.bullets.iterator()
        while (bulletIt.hasNext()) {
            val bullet = bulletIt.next()
            val enemyIt = state.enemies.iterator()

            while (enemyIt.hasNext()) {
                val enemy = enemyIt.next()
                val apparentSegment = apparentSegment(enemy)

                if (apparentSegment !== bullet.segment) {
                    continue
                }

                /* Continuous collision detection, as our fast moving bullets will often pass through
                 * enemies for any given frame:
                 *   t1:   [B] ->   <- [E]
                 *   t2:       <- [E] [B] ->
                 *
                 * For each bullet-enemy pair (on the same segment), simulate the movement of each
                 * without actually moving them, then check if the bullet passed through.
                 */

                // Only include bullets yet to pass beyond the enemy, no
                // need to even simulate any movement for these, they will not hit.
                if (bullet.zPosition > enemy.zPosition) {
                    continue
                }

                if (
                    // Either they are overlapping right now, before any simulated movement...
                    bullet.zPosition >= enemy.zPosition

                    ||

                    // ... or once we advance the bullet and the enemy toward each other, the
                    // bullet is now either overlapping or beyond the enemy.
                    (bullet.zPosition + TempestGameState.BULLET_SPEED * delta) >
                        (enemy.zPosition - state.enemySpeed * delta * if (enemy is EnemyBullet) TempestGameState.ENEMY_BULLET_VS_SPEED_RATIO else 1f)

                ) {

                    bulletIt.remove()

                    if (enemy is Spike) {
                        enemy.zPosition += TempestGameState.SPIKE_LENGTH_LOSS_PER_HIT
                        if (enemy.zPosition >= TempestGameState.LEVEL_DEPTH) {
                            enemyIt.remove()
                        }
                    } else {
                        when (enemy) {
                            is EnemyBullet -> { }
                            is FlipperTanker -> increaseScore(TempestGameState.SCORE_PER_FLIPPER_TANKER)
                            is Flipper -> increaseScore(TempestGameState.SCORE_PER_FLIPPER)
                            is SpikeBuilder -> increaseScore(TempestGameState.SCORE_PER_SPIKE_BUILDER)
                            is Spike -> { } // Handled above separately (no explosions)
                        }

                        queueExplosion(exactEnemyPosition(enemy), enemy.zPosition)
                        enemyIt.remove()
                        hitEnemies.add(enemy)
                    }

                    break

                }

            }
        }

        if (hitEnemies.isNotEmpty()) {
            sounds.enemyHit()
        }

        onEnemiesRemoved(hitEnemies)
    }

    private fun onEnemiesRemoved(enemies: List<Enemy>) {
        for (enemy in enemies) {
            when (enemy) {
                is FlipperTanker -> {
                    spawnFlippersFromTanker(enemy)
                    maybeSpawnFromPool(state.poolOfFlipperTankers)
                }

                is Flipper -> {
                    maybeSpawnFromPool(state.poolOfFlippers)
                }

                is EnemyBullet -> {} // No need to do anything specific here.
                is Spike -> {}
                is SpikeBuilder -> {
                    // If we are removing the spike builder not because it got hit, but rather
                    // because it got back to its original location, then spawn a flipper tanker.
                    if (enemy.direction == ZDirection.Retreating && enemy.zPosition >= TempestGameState.LEVEL_DEPTH) {
                        spawnFlipperTankerFromSpikeBuilder()
                    }

                    maybeSpawnFromPool(state.poolOfSpikeBuilders)?.let {
                        state.enemies += it.spike
                    }
                }
            }
        }
    }

    private fun <T: Enemy>maybeSpawnFromPool(pool: LinkedList<out T>): T? {
        if (pool.isEmpty()) {
            return null
        }

        val newEnemy = pool.pop()
        state.enemies += newEnemy
        state.enemies += EnemyBullet(newEnemy.segment, newEnemy.zPosition)

        sounds.enemySpawn()

        return newEnemy
    }

    private fun spawnFlipperTankerFromSpikeBuilder() {
        state.enemies.add(FlipperTanker(state.level.segments.random()))
    }

    private fun spawnFlippersFromTanker(enemy: FlipperTanker) {
        // One of these two directions will be non-null (potentially both, but at least one).
        val clockwise = enemy.segment.next(Direction.Clockwise)
        val counterClockwise = enemy.segment.next(Direction.CounterClockwise)

        val nextSegmentClockwise = clockwise ?: counterClockwise!!
        val nextSegmentCounterClockwise = counterClockwise ?: clockwise!!

        val newEnemies = listOf(
            Flipper(nextSegmentClockwise, enemy.zPosition, state.enemyFlipWaitTime),
            Flipper(nextSegmentCounterClockwise, enemy.zPosition, state.enemyFlipWaitTime),
        )

        state.enemies += newEnemies

        if (state.networkEnemies.contains(enemy)) {
            state.networkEnemies += newEnemies
        }
    }

    private fun updateExplosions() {
        state.explosions.removeAll { state.timer > it.startTime + TempestGameState.EXPLOSION_TIME }
    }

    private fun checkEndLevel() {
        if (
            getState() == State.Playing &&
            state.poolOfFlippers.isEmpty() &&
            state.poolOfFlipperTankers.isEmpty() &&
            state.enemies.none { it !is Spike && it !is EnemyBullet }
        ) {
            state.nextLevelTime = state.timer + TempestGameState.TOTAL_TIME_BETWEEN_LEVELS
            sounds.levelWarp()
        }
    }

    /**
     * Return false if we hit a spike (and hence need to restart the level again).
     */
    private fun advancePlayer(): Boolean {
        if (state.nextLevelTime - state.timer < TempestGameState.LEVEL_END_TRANSIT_TIME) {
            val percent = 1f - (state.nextLevelTime - state.timer) / TempestGameState.LEVEL_END_TRANSIT_TIME
            state.playerDepth = TempestGameState.LEVEL_DEPTH * percent

            val hitSpike = state.enemies.find {
                it is Spike && // All enemies at this stage should be a spike, but doesn't hurt to ask...
                it.segment === state.playerSegment &&
                it.zPosition < state.playerDepth
            }

            if (hitSpike != null) {
                onPlayerHit()
                return false
            }
        }

        return true
    }

    private fun maybeAdvanceLevel() {
        if (state.timer > state.nextLevelTime) {
            Gdx.app.log(TAG, "Advancing to next level.")

            val currentLevelIndex = state.allLevels.indexOf(state.level)
            state.level = state.allLevels[(currentLevelIndex + 1) % state.allLevels.size]

            state.bullets.clear()
            state.playerSegment = state.level.segments[0]
            state.playerDepth = 0f
            state.levelCount ++
            state.nextLevelTime = 0f

            state.poolOfFlippers.clear()
            state.poolOfFlipperTankers.clear()

            state.numSpawnedFromPoolFlippers = 0
            state.numSpawnedFromPoolFlipperTankers = 0
            state.numSpawnedFromPoolSpikeBuilder = 0

            state.networkEnemies.clear()

            state.increaseSpeed()
            initEnemyPool()
        }
    }

    private fun maybeRespawnPlayer() {
        if (state.timer > state.nextPlayerRespawnTime) {
            Gdx.app.log(TAG, "Respawning player.")
            state.bullets.clear()
            state.enemies.clear()
            state.playerSegment = state.level.segments[0]
            state.playerDepth = 0f
            state.nextEnemyTime = TempestGameState.PAUSE_AFTER_DEATH
            state.nextPlayerRespawnTime = 0f

            state.numSpawnedFromPoolFlippers = 0
            state.numSpawnedFromPoolFlipperTankers = 0
            state.numSpawnedFromPoolSpikeBuilder = 0

            // TODO: Don't clear the pool of enemies, because in the original tempest a death doesn't
            // restart the level - it remembers how many you killed and kind of "resumes" from
            // where you left off...
            initEnemyPool()
        }
    }

    private fun moveEnemies(delta: Float) {
        val enemyIt = state.enemies.iterator()
        val toRemove = mutableListOf<Enemy>()
        while (enemyIt.hasNext()) {
            val enemy = enemyIt.next()
            val remove = when (enemy) {
                is Flipper -> moveFlipper(delta, enemy)
                is FlipperTanker -> moveFlipperTanker(delta, enemy)
                is EnemyBullet -> moveEnemyBullet(delta, enemy)
                is SpikeBuilder -> moveSpikeBuilder(delta, enemy)
                is Spike -> false
            }

            if (remove) {
                toRemove.add(enemy)
                enemyIt.remove()
            }
        }

        onEnemiesRemoved(toRemove)
    }

    private fun initEnemyPool() {

        // Most enemies should already be gone from here (because we advanced to the next level),
        // however spikes may still exist. Need to clear these before the next level starts.
        state.enemies.clear()

        state.poolOfFlippers.clear()
        state.poolOfFlipperTankers.clear()
        state.poolOfSpikeBuilders.clear()

        val makeCounter = { startLevel: Int, startAmount: Int, amountPerLevel: Int, maxNumber: Int ->
            if (startLevel > state.levelCount) {
                emptyList()
            } else {
                val end = startAmount + (state.levelCount - startLevel) * amountPerLevel
                (0 until end.coerceAtMost(maxNumber))
            }
        }

        state.poolOfFlippers += makeCounter(0, 10, 1, 15).map {
            val segment = state.level.segments.random()
            Flipper(
                segment,
                TempestGameState.LEVEL_DEPTH,

                // On level 1, the flippers don't actually flip, so set a really high "time until flip"
                // All other levels, increase the speed as levels increase.
                if (state.levelCount == 0) { 10000f } else { state.enemyFlipWaitTime },
            )
        }

        state.poolOfFlipperTankers += makeCounter(2, 2, 1, 6).map {
            FlipperTanker(state.level.segments.random())
        }

        val segmentsForSpikes = state.level.segments.shuffled()
        state.poolOfSpikeBuilders += makeCounter(3, 3, 1, 12).map { i ->
            SpikeBuilder(segmentsForSpikes[i])
        }

        // For if we accumulated any attacks over the network while transitioning levels or waiting
        // for the player to respawn...
        spawnNetworkEnemies(state.numQueuedNetworkEnemies)
        state.numQueuedNetworkEnemies = 0

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
            else enemy.segment.next(enemy.direction) ?: enemy.segment

        else -> enemy.segment
    }

    private fun moveFlipper(delta: Float, enemy: Flipper): Boolean {
        enemy.timeUntilNextFlip -= delta

        if (enemy.timeUntilNextFlip < 0) {
            enemy.segment = enemy.segment.next(enemy.direction)!!

            if (enemy.segment.next(enemy.direction) == null) {
                enemy.direction = oppositeDirection(enemy.direction)
            }

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
                    val newDirection = oppositeDirection(enemy.direction)
                    if (enemy.segment.next(newDirection) != null) {
                        enemy.direction = newDirection
                    }
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

        if (enemy.zPosition <= 0 && enemy.segment === state.playerSegment) {
            onPlayerHit()
            return true
        }

        return false
    }

    private fun moveFlipperTanker(delta: Float, enemy: FlipperTanker): Boolean {
        enemy.zPosition -= state.enemySpeed * delta

        if (enemy.zPosition <= 0) {
            if (enemy.segment === state.playerSegment) {
                onPlayerHit()
            }
            return true
        }

        return false
    }

    private fun moveSpikeBuilder(delta: Float, enemy: SpikeBuilder): Boolean {
        if (enemy.direction == ZDirection.Advancing) {
            enemy.zPosition -= state.enemySpeed * delta
            enemy.spike.zPosition = enemy.zPosition + 0.01f // Ever so slightly further back, so a bullet should hit the spike builder before the spike.
            if (enemy.zPosition <= TempestGameState.LEVEL_DEPTH / 5f) {
                enemy.direction = ZDirection.Retreating
            }
        } else {
            enemy.zPosition += state.enemySpeed * delta
            if (enemy.zPosition >= TempestGameState.LEVEL_DEPTH) {
                return true
            }
        }

        return false
    }

    private fun moveEnemyBullet(delta: Float, bullet: EnemyBullet): Boolean {
        bullet.zPosition -= state.enemySpeed * delta * TempestGameState.ENEMY_BULLET_VS_SPEED_RATIO

        if (bullet.zPosition <= 0) {
            if (bullet.segment === state.playerSegment) {
                onPlayerHit()
            }
            return true
        }

        return false
    }

    private fun queueExplosion(position: Vector2, zPosition: Float) {
        // March enemies forward toward the screen...
        state.explosions.add(Explosion(Vector3(position.x, position.y, zPosition), state.timer))
    }

    private fun onPlayerHit() {
        // We have already been hit and are waiting to spawn again, no need to check again.
        if (state.nextPlayerRespawnTime > state.timer) {
            return
        }

        sounds.playerHit()
        state.numLives --
        state.nextLevelTime = 0f
        queueExplosion(state.playerSegment.centre, state.playerDepth)
        queueExplosion(state.playerSegment.centre.cpy().add(1.5f, 1.5f), state.playerDepth)
        queueExplosion(state.playerSegment.centre.cpy().add(-1.5f, -1.5f), state.playerDepth)
        queueExplosion(state.playerSegment.centre.cpy().add(-1.5f, 1.5f), state.playerDepth)
        queueExplosion(state.playerSegment.centre.cpy().add(1.5f, -1.5f), state.playerDepth)

        state.nextPlayerRespawnTime = state.timer + TempestGameState.PAUSE_AFTER_DEATH
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
        if (controller!!.trigger(TempestSoftController.Buttons.MOVE_CLOCKWISE)) {
            state.playerSegment = state.playerSegment.next(Direction.Clockwise) ?: state.playerSegment
        }

        if (controller.trigger(TempestSoftController.Buttons.MOVE_COUNTER_CLOCKWISE)) {
            state.playerSegment = state.playerSegment.next(Direction.CounterClockwise) ?: state.playerSegment
        }
    }

    private fun fire() {
        val isFiring = controller!!.trigger(TempestSoftController.Buttons.FIRE)
        if (!isFiring || state.bullets.size >= TempestGameState.MAX_PLAYER_BULLETS_AT_ONCE) {
            return
        }

        sounds.playerFire()

        val flippersToHit = mutableListOf<Flipper>()
        for (enemy in state.enemies) {
            if (enemy is Flipper && enemy.zPosition <= 0 && apparentSegment(enemy) === state.playerSegment) {
                flippersToHit.add(enemy)
            }
        }
        flippersToHit.sortBy { it.timeUntilNextFlip }

        if (flippersToHit.isNotEmpty()) {
            // Just kill it directly without even showing the bullet. We are right on top of this enemy.
            sounds.enemyHit()
            state.enemies.remove(flippersToHit[0])
            increaseScore(TempestGameState.SCORE_PER_FLIPPER)
            queueExplosion(exactEnemyPosition(flippersToHit[0]), 0f)

            onEnemiesRemoved(flippersToHit.subList(0, 1))
        } else {
            state.bullets.add(Bullet(state.playerSegment, state.playerDepth))
        }
    }

    override fun onReceiveDamage(strength: Int) {
        val numEnemies = strength * 2
        if (state.nextLevelTime > 0 || state.nextPlayerRespawnTime > 0) {
            Gdx.app.debug(TAG, "Queuing up enemy attacks from network because we are not currently in a a state to receive them.")
            state.numQueuedNetworkEnemies += numEnemies
        } else {
            Gdx.app.debug(TAG, "Applying network attack immediately.")
            spawnNetworkEnemies(numEnemies)
        }
    }

    private fun spawnNetworkEnemies(numEnemies: Int) {
        Gdx.app.debug(TAG, "Spawning $numEnemies enemies from network attacks.")
        val flipperTankers = (0 until numEnemies).map {
            FlipperTanker(state.level.segments.random())
        }

        state.enemies += flipperTankers
        state.networkEnemies += flipperTankers
    }

    override fun setupCamera(camera: Camera, yOffset: Float) {
        camera.apply {
            val startZ = viewport.worldHeight.coerceAtMost(viewport.worldWidth) // Arbitrarily chosen depth which seems to work well.
            val cameraZ: Float
            val lookAtZ: Float
            if (state.nextLevelTime > 0 && state.nextLevelTime - state.timer < TempestGameState.LEVEL_END_TRANSIT_TIME) {
                val percent = 1 - ((state.nextLevelTime - state.timer) / TempestGameState.LEVEL_END_TRANSIT_TIME)

                // Make this lag a little bit so we can still see the player as we advance forward.
                val lagBehindPlayer = (TempestGameState.LEVEL_DEPTH / 3)
                val endZ = lagBehindPlayer
                val amountMoved = (endZ + startZ + lagBehindPlayer) * percent

                cameraZ = (startZ - amountMoved).coerceAtMost(startZ) // Because we want to lag, but the camera normally sits on the player...
                lookAtZ = -(TempestGameState.LEVEL_DEPTH * 5) * percent
            } else {
                cameraZ = startZ
                lookAtZ = 0f
            }

            position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f + state.level.cameraOffset + yOffset * 0.5f, cameraZ)
            lookAt(viewport.worldWidth / 2f, viewport.worldHeight / 2f + yOffset * 0.5f, lookAtZ)
            update()
        }
    }

    override fun renderGame(camera: Camera) {
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

    private fun renderOnAngle(shapeRenderer: ShapeRenderer, pos: Vector2, zPosition: Float, angleInDegrees: Float, block: (shapeRenderer: ShapeRenderer) -> Unit) {
        shapeRenderer.translate(pos.x, pos.y, zPosition)
        shapeRenderer.rotate(0f, 0f, 1f, angleInDegrees)
        shapeRenderer.rotate(0f, 1f, 0f, 180f)
        shapeRenderer.rotate(1f, 0f, 0f, 90f)
        block(shapeRenderer)
        shapeRenderer.identity()
    }

    private fun renderPlayer(shapeRenderer: ShapeRenderer) {
        renderOnAngle(shapeRenderer, state.playerSegment.centre, -state.playerDepth, state.playerSegment.angle) {
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

    private fun renderEnemy(shapeRenderer: ShapeRenderer, enemy: Enemy) = when (enemy) {
        is Flipper -> renderFlipper(shapeRenderer, enemy)
        is FlipperTanker -> renderFlipperTanker(shapeRenderer, enemy)
        is EnemyBullet -> renderEnemyBullet(shapeRenderer, enemy)
        is Spike -> renderSpike(shapeRenderer, enemy)
        is SpikeBuilder -> renderSpikeBuilder(shapeRenderer, enemy)
    }

    private fun renderSpikeBuilder(shapeRenderer: ShapeRenderer, spikeBuilder: SpikeBuilder) {
        shapeRenderer.color = Color.GREEN
        shapeRenderer.translate(spikeBuilder.segment.centre.x, spikeBuilder.segment.centre.y, -spikeBuilder.zPosition)
        shapeRenderer.polyline(SpikeBuilder.vertices)
        // TODO: Offset by half the width of the spiral
        shapeRenderer.identity()
    }

    private fun renderSpike(shapeRenderer: ShapeRenderer, spike: Spike) {
        shapeRenderer.color = Color.GREEN
        shapeRenderer.line(
            spike.segment.centre.x, spike.segment.centre.y, -TempestGameState.LEVEL_DEPTH,
            spike.segment.centre.x, spike.segment.centre.y, -spike.zPosition,
        )
        shapeRenderer.box(
            spike.segment.centre.x - 0.25f, spike.segment.centre.y - 0.25f, -spike.zPosition - 0.25f,
            0.5f, 0.5f, 0.5f,
        )
    }

    private fun renderEnemyBullet(shapeRenderer: ShapeRenderer, bullet: EnemyBullet) {
        shapeRenderer.color = if (state.networkEnemies.contains(bullet)) ENEMY_ATTACK_COLOUR else Color.WHITE
        val angle = (state.timer % 3) * 360

        shapeRenderer.apply {

            translate(bullet.segment.centre.x, bullet.segment.centre.y, -bullet.zPosition)
            rotate(0f, 0f, 1f, angle)

            /**
             *            1 c
             *    b       |        d
             *            |
             *  a         |
             * -1---------+----------1-
             *            |          e
             *            |
             *    h       |      f
             *          g 1
             */
            line(-1f, 0.25f, 0f, -0.75f, 0.75f, 0f)
            line(0.25f, 1f, 0f, 0.75f, 0.75f, 0f)
            line(1f, -0.25f, 0f, 0.75f, -0.75f, 0f)
            line(-0.25f, -1f, 0f, -0.75f, -0.75f, 0f)

            identity()

        }
    }

    private fun renderFlipper(shapeRenderer: ShapeRenderer, enemy: Flipper) {
        shapeRenderer.color = if (state.networkEnemies.contains(enemy)) ENEMY_ATTACK_COLOUR else Color.WHITE
        if (enemy.timeUntilNextFlip > state.enemyFlipTransitionTime /* Not yet moving to the next segment */) {
            renderOnAngle(shapeRenderer, enemy.segment.centre, -enemy.zPosition, enemy.segment.angle) {
                it.polygon(flipperShape)
            }
            enemy.isFlipping = false
        } else {
            if (!enemy.isFlipping) {
                sounds.enemyFlip()
                enemy.isFlipping = true
            }

            val flipPercent = 1f - enemy.timeUntilNextFlip / state.enemyFlipTransitionTime

            // When we assigned the direction in the past, we ensured the next segment is not null
            // before assigning, so we are very confident that it is not null here.
            val nextSegment = enemy.segment.next(enemy.direction) ?: throw IllegalStateException("Should always have a next direction.")
            val pos = enemy.segment.centre.cpy().add(nextSegment.centre.cpy().sub(enemy.segment.centre).scl(flipPercent))
            val angle = enemy.segment.angle + (flipPercent * if (enemy.direction == Direction.Clockwise) -180f else 180f)

            renderOnAngle(shapeRenderer, pos, -enemy.zPosition, angle) {
                it.polygon(flipperShape)
            }
        }
    }

    private fun renderFlipperTanker(shapeRenderer: ShapeRenderer, enemy: FlipperTanker) {
        shapeRenderer.color = if (state.networkEnemies.contains(enemy)) ENEMY_ATTACK_COLOUR else Color.WHITE
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

            // When we assigned the direction in the past, we ensured the next segment is not null
            // before assigning, so we are very confident that it is not null here.
            val nextSegment = enemy.segment.next(enemy.direction)!!

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
