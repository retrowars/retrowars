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

        recordInput()
        movePlayer()
        moveBullets(delta)
        moveEnemies(delta)
        maybeSpawnEnemies()
        fire()
        resetInput()
    }

    private fun maybeSpawnEnemies() {
        if (getState() == State.Playing && state.numEnemiesRemaining <= 0) {
            if (state.enemies.size == 0) {
                // completeLevel()
            }
        } else if (state.shouldSpawnEnemy()) {
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
        val enemiesToMove = state.enemies
            .onEach { it.timeUntilMove -= delta }
            .filter { it.timeUntilMove < 0 }
            .onEach { it.timeUntilMove = Enemy.STEP_DURATION }

        // Enemies at the end of the screen are crawling. In this mode, they move around clockwise
        // or counterclockwise towards the player. After a certain number of moves in this mode
        // they will disappear.
        val toRemove = enemiesToMove
            .filter { it.state == Enemy.State.Crawling }
            .onEach {
                val currentEnemyIndex = state.level.segments.indexOf(it.segment)
                it.segment = when(it.direction) {
                    Enemy.Direction.Clockwise -> state.level.segments[(state.level.segments.size + currentEnemyIndex - 1) % state.level.segments.size]
                    Enemy.Direction.CounterClockwise -> state.level.segments[(currentEnemyIndex + 1) % state.level.segments.size]
                }
                it.crawlsRemaining --
            }
            .filter { it.crawlsRemaining < 0 }

        state.enemies.removeAll(toRemove)

        // March enemies forward toward the screen...
        enemiesToMove
            .filter { it.state == Enemy.State.Walking }
            .onEach {
                it.depth -= TempestGameState.LEVEL_DEPTH / 10
            }
            // ... when they have walked to the end of the screen, tell them they are now crawling.
            .filter { it.depth <= 0 }
            .onEach {
                it.state = Enemy.State.Crawling
                it.depth = 0f
            }
    }

    private fun moveBullets(delta: Float) {
        state.bullets
            .onEach { it.depth += TempestGameState.BULLET_SPEED * delta }
            .removeAll { it.depth > TempestGameState.LEVEL_DEPTH }
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
        if (state.fire == ButtonState.JustPressed) {
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
        shapeRenderer.box(enemy.segment.centre.x, enemy.segment.centre.y, -enemy.depth, 2f, 2f, 1f)
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

}
