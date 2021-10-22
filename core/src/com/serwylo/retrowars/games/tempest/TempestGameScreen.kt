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

        state.enemies.add(makeEnemy(state.level.segments[0]))

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
        maybeSpawnEnemies()
        updateExplosions()
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
            state.numEnemiesRemaining = TempestGameState.BASE_ENEMIES_PER_LEVEL + state.levelCount
            state.nextLevelTime = 0f
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
        state.enemies.add(makeEnemy(state.level.segments.random()))
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
        val enemiesToKill = mutableListOf<Enemy>()
        val bulletsToRemove = mutableListOf<Bullet>()

        state.enemies
            .onEach { it.timeUntilNextCrawl -= delta }
            .filter { it.timeUntilNextCrawl < 0 }
            .onEach {
                it.segment = getNextSegment(it)
                it.timeUntilNextCrawl = TempestGameState.ENEMY_CRAWL_WAIT_TIME

                if (it.depth > 0) {
                    // Before getting to the end of the screen, enemies sometimes crawl, and
                    // sometimes don't. If they do, they seem to for an arbitrary period before
                    // stopping and then beginning crawling again in the future.
                    val waitBeforeCrawlingAgain = Random.nextFloat() > 0.3f
                    if (waitBeforeCrawlingAgain) {
                        it.timeUntilNextCrawl += Random.nextInt(0, 2000) / 1000f
                    }

                    val changeDirection = Random.nextFloat() > 0.8f
                    if (changeDirection) {
                        it.direction = when (it.direction) {
                            Enemy.Direction.Clockwise -> Enemy.Direction.CounterClockwise
                            Enemy.Direction.CounterClockwise -> Enemy.Direction.Clockwise
                        }
                    }
                }
            }

        // March enemies forward toward the screen...
        state.enemies
            .filter { it.depth > 0 }
            .onEach { enemy ->

                // A bit hackey, but we do this check here before advancing the player forward, and
                // then we'll do the same when advancing the bullet forward later.
                val closestBullet = state.bullets
                    .filter { bullet -> bullet.segment === enemy.segment && bullet.depth < enemy.depth }
                    .maxByOrNull { it.depth }

                val newDepth = enemy.depth - TempestGameState.ENEMY_SPEED * delta
                if (closestBullet != null) {
                    if (closestBullet.depth > newDepth) {
                        enemiesToKill.add(enemy)
                        bulletsToRemove.add(closestBullet)
                    }
                }

                enemy.depth = newDepth

            }
            // ... when they have walked to the end of the screen, tell them they are now crawling.
            .filter { it.depth <= 0 }
            .onEach {
                it.depth = 0f

                // If we were in the middle of waitign some time before crawling, ignore that and
                // queue up the next crawl at the approved interval.
                it.timeUntilNextCrawl = it.timeUntilNextCrawl.coerceAtMost(TempestGameState.ENEMY_CRAWL_WAIT_TIME)
            }

        state.bullets.removeAll(bulletsToRemove)
        killEnemiesAndScore(enemiesToKill)
    }

    private fun queueExplosion(position: Vector2, depth: Float) {
        state.explosions.add(Explosion(Vector3(position.x, position.y, depth), state.timer))
    }

    private fun checkIfPlayerHit() {
        val enemies = state.enemies.filter {
            it.depth <= 0 && it.segment == state.playerSegment
        }

        if (enemies.isNotEmpty()) {
            state.numLives --
            queueExplosion(state.playerSegment.centre, enemies[0].depth)
            queueExplosion(state.playerSegment.centre.cpy().add(1.5f, 1.5f), enemies[0].depth)
            queueExplosion(state.playerSegment.centre.cpy().add(-1.5f, -1.5f), enemies[0].depth)
            queueExplosion(state.playerSegment.centre.cpy().add(-1.5f, 1.5f), enemies[0].depth)
            queueExplosion(state.playerSegment.centre.cpy().add(1.5f, -1.5f), enemies[0].depth)

            state.nextPlayerRespawnTime = state.timer + TempestGameState.PAUSE_AFTER_DEATH
        }
    }

    private fun moveBullets(delta: Float) {
        val enemiesToKill = mutableListOf<Enemy>()
        val bulletsToRemove = mutableListOf<Bullet>()

        state.bullets
            .onEach { bullet ->

                // A bit hackey, but we do this check here before advancing the player forward, and
                // then we'll do the same when advancing the bullet forward later.
                val closestEnemy = state.enemies
                    .filter { enemy -> enemy.depth > bullet.depth }
                    .filter { enemy ->
                        // Either not crawling, or crawling but hasn't passed half way between segments yet, so just look at its normal segment.
                        enemy.timeUntilNextCrawl > TempestGameState.ENEMY_CRAWL_TRANSITION_TIME / 2 && enemy.segment === bullet.segment
                        ||
                        // Started crawling and past half way, so consider it in its adjacent segment.
                        enemy.timeUntilNextCrawl <= TempestGameState.ENEMY_CRAWL_TRANSITION_TIME / 2 && getNextSegment(enemy) === bullet.segment
                    }
                    .minByOrNull { it.depth }

                val newDepth = bullet.depth + TempestGameState.BULLET_SPEED * delta

                if (closestEnemy != null) {
                    if (closestEnemy.depth < newDepth) {
                        enemiesToKill.add(closestEnemy)
                        bulletsToRemove.add(bullet)
                    }
                }

                bullet.depth = newDepth
            }
            .removeAll { it.depth > TempestGameState.LEVEL_DEPTH }

        state.bullets.removeAll(bulletsToRemove)
        killEnemiesAndScore(enemiesToKill)
    }

    private fun killEnemiesAndScore(enemiesToKill: Collection<Enemy>) {
        increaseScore(enemiesToKill.size * TempestGameState.SCORE_PER_ENEMY)
        state.enemies.removeAll(enemiesToKill)
        enemiesToKill.onEach {
            queueExplosion(it.segment.centre, it.depth)
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

        val enemyMovingHere = state.enemies
            .asSequence()
            .filter { it.depth <= 0 }
            .filter { it.timeUntilNextCrawl < TempestGameState.ENEMY_CRAWL_TRANSITION_TIME } // Is transitioning from one segment to the next
            .filter { getNextSegment(it) == state.playerSegment }
            .sortedBy { it.timeUntilNextCrawl } // Shoot the closest ones first...
            .firstOrNull()

        if (enemyMovingHere != null) {
            // Just kill it directly without even showing the bullet. We are right on top of this enemy.
            state.enemies.remove(enemyMovingHere)
            increaseScore(TempestGameState.SCORE_PER_ENEMY)
            queueExplosion(state.playerSegment.centre, 0f)
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

        r.color = Color.RED
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

    private fun renderPlayer(shapeRenderer: ShapeRenderer) {
        shapeRenderer.translate(state.playerSegment.centre.x, state.playerSegment.centre.y, 0f)
        shapeRenderer.rotate(0f, 0f, 1f, state.playerSegment.angle)
        shapeRenderer.rotate(0f, 1f, 0f, 180f)
        shapeRenderer.rotate(1f, 0f, 0f, 90f)
        shapeRenderer.polygon(playerShape)
        shapeRenderer.identity()
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
        val max = TempestGameState.MAX_TIME_BETWEEN_ENEMIES
        val min = TempestGameState.MIN_TIME_BETWEEN_ENEMIES
        state.nextEnemyTime = state.timer + ((Math.random() * (max - min)) + min).toFloat()
    }

    private fun renderEnemy(shapeRenderer: ShapeRenderer, enemy: Enemy) {
        if (enemy.timeUntilNextCrawl > TempestGameState.ENEMY_CRAWL_TRANSITION_TIME /* Not yet moving to the next segment */) {
            shapeRenderer.translate(enemy.segment.centre.x, enemy.segment.centre.y, -enemy.depth)
            shapeRenderer.rotate(0f, 0f, 1f, enemy.segment.angle)
            shapeRenderer.box(-1f, -0.25f, -1f, 3f, 0.5f, 1f)
            shapeRenderer.identity()
        } else {

            val crawlPercent = 1f - enemy.timeUntilNextCrawl / TempestGameState.ENEMY_CRAWL_TRANSITION_TIME
            val nextSegment = getNextSegment(enemy)

            val pos = enemy.segment.centre.cpy().add(nextSegment.centre.cpy().sub(enemy.segment.centre).scl(crawlPercent))
            val angle = enemy.segment.angle + (crawlPercent * if (enemy.direction == Enemy.Direction.Clockwise) 180f else -180f)
            shapeRenderer.translate(pos.x, pos.y, -enemy.depth)
            shapeRenderer.rotate(0f, 0f, 1f, angle)
            shapeRenderer.box(-1f, -0.25f, -1f, 3f, 0.5f, 1f)
            shapeRenderer.identity()
        }
    }

    private fun renderBullet(shapeRenderer: ShapeRenderer, bullet: Bullet) {
        shapeRenderer.box(bullet.segment.centre.x, bullet.segment.centre.y, -bullet.depth, 1f, 1f, 1f)
    }

    private fun renderSegment(shapeRenderer: ShapeRenderer, segment: Segment) {
        shapeRenderer.line(segment.start.x, segment.start.y, -TempestGameState.LEVEL_DEPTH, segment.end.x, segment.end.y, -TempestGameState.LEVEL_DEPTH)
        shapeRenderer.line(segment.start.x, segment.start.y, 0f, segment.start.x, segment.start.y, -TempestGameState.LEVEL_DEPTH)
        shapeRenderer.line(segment.end.x, segment.end.y, 0f, segment.end.x, segment.end.y, -TempestGameState.LEVEL_DEPTH)
        shapeRenderer.line(segment.start, segment.end)
    }

    private fun getNextSegment(enemy: Enemy): Segment {
        val currentEnemyIndex = state.level.segments.indexOf(enemy.segment)
        return when(enemy.direction) {
            Enemy.Direction.Clockwise -> state.level.segments[(state.level.segments.size + currentEnemyIndex - 1) % state.level.segments.size]
            Enemy.Direction.CounterClockwise -> state.level.segments[(currentEnemyIndex + 1) % state.level.segments.size]
        }
    }

}
