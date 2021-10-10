package com.serwylo.retrowars.games.tempest

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.tetris.ButtonState
import com.serwylo.retrowars.input.TempestSoftController

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
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
        state.timer += delta

        checkEndLevel()

        recordInput()
        movePlayer()
        moveBullets(delta)
        moveEnemies(delta)
        maybeSpawnEnemies()
        fire()
        resetInput()
    }

    private fun checkEndLevel() {
        if (getState() == State.Playing && state.numEnemiesRemaining == 0 && state.enemies.isEmpty()) {
            state.bullets.clear()
            state.level = state.allLevels.filter { it != state.level }.random()
            state.playerSegment = state.level.segments[0]
            state.levelCount ++
            state.numEnemiesRemaining = TempestGameState.BASE_ENEMIES_PER_LEVEL + state.levelCount
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

        // Enemies at the end of the screen are crawling. In this mode, they move around clockwise
        // or counterclockwise towards the player. After a certain number of moves in this mode
        // they will disappear.
        val toRemove = state.enemies
            .filter { it.state == Enemy.State.Crawling }
            .onEach {
                it.timeUntilNextCrawl -= delta
            }
            .filter { it.timeUntilNextCrawl < 0 }
            .onEach {
                it.timeUntilNextCrawl = TempestGameState.ENEMY_CRAWL_WAIT_TIME

                it.segment = getNextSegment(it)

                it.crawlsRemaining --

            }
            .filter { it.crawlsRemaining < 0 }

        state.enemies.removeAll(toRemove)

        // March enemies forward toward the screen...
        state.enemies
            .filter { it.state == Enemy.State.Walking }
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
                it.state = Enemy.State.Crawling
                it.depth = 0f
            }

        state.bullets.removeAll(bulletsToRemove)
        killEnemiesAndScore(enemiesToKill)
    }

    private fun moveBullets(delta: Float) {
        val enemiesToKill = mutableListOf<Enemy>()
        val bulletsToRemove = mutableListOf<Bullet>()

        state.bullets
            .onEach { bullet ->

                // A bit hackey, but we do this check here before advancing the player forward, and
                // then we'll do the same when advancing the bullet forward later.
                val closestEnemy = state.enemies
                    .filter { enemy -> enemy.segment === bullet.segment && enemy.depth > bullet.depth }
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
    }

    private fun movePlayer() {
        val currentIndex = state.level.segments.indexOf(state.playerSegment)

        if (state.moveClockwise == ButtonState.JustPressed) {
            state.playerSegment = state.level.segments[(currentIndex + 1) % state.level.segments.size]
        } else if (state.moveCounterClockwise == ButtonState.JustPressed) {
            state.playerSegment = state.level.segments[(state.level.segments.size + currentIndex - 1) % state.level.segments.size]
        }
    }

    private fun fire() {
        if (state.fire != ButtonState.JustPressed) {
            return
        }

        val enemyMovingHere = state.enemies
            .asSequence()
            .filter { it.state == Enemy.State.Crawling }
            .filter { it.timeUntilNextCrawl < TempestGameState.ENEMY_CRAWL_TRANSITION_TIME } // Is transitioning from one segment to the next
            .filter { getNextSegment(it) == state.playerSegment }
            .sortedBy { it.timeUntilNextCrawl } // Shoot the closest ones first...
            .firstOrNull()

        if (enemyMovingHere != null) {
            // Just kill it directly without even showing the bullet. We are right on top of this enemy.
            state.enemies.remove(enemyMovingHere)
            // TODO: Queue up some sort of dying feedback so we know we at least hit it (doesn't need to be glamourous for now).
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
    }

    private fun renderLevel(camera: Camera) {
        val r = game.uiAssets.shapeRenderer

        r.begin(ShapeRenderer.ShapeType.Line)
        r.projectionMatrix = camera.combined

        r.color = Color.BLUE
        state.level.segments.forEach { segment ->
            renderSegment(r, segment)
        }

        r.color = Color.YELLOW
        renderSegment(r, state.playerSegment)

        state.bullets.onEach {
            renderBullet(r, it)
        }

        r.color = Color.RED
        state.enemies.onEach {
            renderEnemy(r, it)
        }
        r.end()
    }

    private fun queueEnemy() {
        val max = TempestGameState.MAX_TIME_BETWEEN_ENEMIES
        val min = TempestGameState.MIN_TIME_BETWEEN_ENEMIES
        state.nextEnemyTime = state.timer + ((Math.random() * (max - min)) + min).toFloat()
    }

    private fun renderEnemy(shapeRenderer: ShapeRenderer, enemy: Enemy) {
        if (enemy.state == Enemy.State.Walking || enemy.timeUntilNextCrawl > TempestGameState.ENEMY_CRAWL_TRANSITION_TIME /* Not yet moving to the next segment */) {
            shapeRenderer.box(enemy.segment.centre.x, enemy.segment.centre.y, -enemy.depth, 2f, 2f, 1f)
        } else {

            val crawlPercent = 1f - enemy.timeUntilNextCrawl / TempestGameState.ENEMY_CRAWL_TRANSITION_TIME
            val nextSegment = getNextSegment(enemy)

            val pos = enemy.segment.centre.cpy().add(nextSegment.centre.cpy().sub(enemy.segment.centre).scl(crawlPercent))
            shapeRenderer.box(pos.x, pos.y, 0f, 2f, 2f, 1f)
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
