package com.serwylo.retrowars.games.snake

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games

class SnakeGameScreen(game: RetrowarsGame) : GameScreen(game, Games.snake, 400f, 400f) {

    companion object {
        @Suppress("unused")
        const val TAG = "SnakeGameScreen"
    }

    private val controllerLeft: Button
    private val controllerRight: Button
    private val controllerUp: Button
    private val controllerDown: Button

    private val state = SnakeGameState()

    /**
     * Used to provide an on-screen controller for driving the ship. Left, Right, Thrust, and Fire.
     */
    private val softController = Table()

    init {

        controllerLeft = TextButton("  <  ", game.uiAssets.getStyles().textButton.huge)
        controllerRight = TextButton("  >  ", game.uiAssets.getStyles().textButton.huge)
        controllerUp = TextButton("  ^  ", game.uiAssets.getStyles().textButton.huge)
        controllerDown = TextButton("  v  ", game.uiAssets.getStyles().textButton.huge)

        controllerLeft.addAction(Actions.alpha(0.4f))
        controllerRight.addAction(Actions.alpha(0.4f))
        controllerUp.addAction(Actions.alpha(0.4f))
        controllerDown.addAction(Actions.alpha(0.4f))

        val buttonSize = UI_SPACE * 15
        softController.apply {
            bottom().pad(UI_SPACE * 4)
            add(controllerLeft).space(UI_SPACE * 2).size(buttonSize)
            add(controllerRight).space(UI_SPACE * 2).size(buttonSize)
            add().expandX()
            add(controllerUp).space(UI_SPACE * 2).size(buttonSize)
            add(controllerDown).space(UI_SPACE * 2).size(buttonSize)
        }

        addGameOverlayToHUD(softController)
        showMessage("Eat the fruit", "Avoid your tail")

    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun getScore() = state.score

    override fun updateGame(delta: Float) {

        state.timer += delta

        state.left = controllerLeft.isPressed || Gdx.input.isKeyPressed(Input.Keys.LEFT)
        state.right = controllerRight.isPressed || Gdx.input.isKeyPressed(Input.Keys.RIGHT)
        state.up = controllerUp.isPressed || Gdx.input.isKeyPressed(Input.Keys.UP)
        state.down = controllerDown.isPressed || Gdx.input.isKeyPressed(Input.Keys.DOWN)

        if (state.left && state.currentDirection != Direction.RIGHT && !state.right && !state.up && !state.down) {
            state.nextDirection = Direction.LEFT
        } else if (state.right && state.currentDirection != Direction.LEFT && !state.left && !state.up && !state.down) {
            state.nextDirection = Direction.RIGHT
        } else if (state.up && state.currentDirection != Direction.DOWN && !state.left && !state.right && !state.down) {
            state.nextDirection = Direction.UP
        } else if (state.down && state.currentDirection != Direction.UP && !state.left && !state.right && !state.up) {
            state.nextDirection = Direction.DOWN
        }

        updateEntities(delta)

        // TODO: Record high score, show end of game screen.
        if (false /* Ran into obstacle */) {
            endGame()
        }

    }

    private fun updateEntities(delta: Float) {
        if (state.timer < state.nextTimeStep) {
            return
        }

        state.nextTimeStep = state.nextTimeStep + state.timeStep

        state.currentDirection = state.nextDirection

        val currentHead = state.snake.first
        val newHead = when(state.nextDirection) {
            Direction.UP -> state.cells[currentHead.y + 1][currentHead.x]
            Direction.DOWN -> state.cells[currentHead.y - 1][currentHead.x]
            Direction.LEFT -> state.cells[currentHead.y][currentHead.x - 1]
            Direction.RIGHT -> state.cells[currentHead.y][currentHead.x + 1]
        }

        Gdx.app.log(TAG, "Moving ${state.currentDirection} from $currentHead -> $newHead")

        if (newHead == state.food) {
            state.timeStep = (state.timeStep - 0.02f).coerceAtLeast(state.minTimeStep)

            val x = (0 until 30).random()
            val y = (0 until 30).random()
            state.food = state.cells[y][x]

            state.score += 10000

        } else {
            state.snake.removeLast()
        }
        state.snake.addFirst(newHead)
    }

    override fun onReceiveDamage(strength: Int) {
    }

    override fun renderGame(camera: OrthographicCamera) {

        val numCellsHigh = state.cells.size
        val numCellsWide = state.cells[0].size

        val cellWidth = viewport.worldWidth / numCellsWide
        val cellHeight = viewport.worldHeight / numCellsHigh

        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = Color.DARK_GRAY

        // For debugging, it can help to draw every cell:
        /*state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                r.rect(x * cellWidth, y * cellHeight, cellWidth, cellHeight)
            }
        }*/

        r.end()
        r.begin(ShapeRenderer.ShapeType.Filled)

        r.color = Color.WHITE
        state.snake.forEach { cell ->
            r.rect(cell.x * cellWidth + 1, cell.y * cellHeight + 1, cellWidth - 2, cellHeight - 2)
        }

        val food = state.food
        r.color = Color.GREEN
        if (food != null) {
            r.rect(food.x * cellWidth + 1, food.y * cellHeight + 1, cellWidth - 2, cellHeight - 2)
        }

        r.end()
    }

}
