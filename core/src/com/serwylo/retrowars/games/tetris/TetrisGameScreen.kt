package com.serwylo.retrowars.games.tetris

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
import com.serwylo.retrowars.games.tetris.entities.Tetronimo
import com.serwylo.retrowars.games.tetris.entities.Tetronimos

class TetrisGameScreen(game: RetrowarsGame) : GameScreen(game, Games.tetris, 400f, 400f) {

    companion object {
        @Suppress("unused")
        const val TAG = "TetrisGameScreen"
    }

    private val controllerMoveLeft: Button
    private val controllerMoveRight: Button
    private val controllerRotateLeft: Button
    private val controllerRotateRight: Button
    private val controllerDrop: Button

    private val state = TetrisGameState()

    /**
     * Used to provide an on-screen controller for driving the ship. Left, Right, Thrust, and Fire.
     */
    private val softController = Table()

    init {

        controllerMoveLeft = TextButton("  <  ", game.uiAssets.getStyles().textButton.huge)
        controllerMoveRight = TextButton("  >  ", game.uiAssets.getStyles().textButton.huge)
        controllerRotateLeft = TextButton("  L  ", game.uiAssets.getStyles().textButton.huge)
        controllerRotateRight = TextButton("  R  ", game.uiAssets.getStyles().textButton.huge)
        controllerDrop = TextButton("  D  ", game.uiAssets.getStyles().textButton.huge)

        controllerMoveLeft.addAction(Actions.alpha(0.4f))
        controllerMoveRight.addAction(Actions.alpha(0.4f))
        controllerRotateLeft.addAction(Actions.alpha(0.4f))
        controllerRotateRight.addAction(Actions.alpha(0.4f))
        controllerDrop.addAction(Actions.alpha(0.4f))

        val buttonSize = UI_SPACE * 15
        softController.apply {
            bottom().pad(UI_SPACE * 4)
            add(controllerDrop).colspan(5).space(UI_SPACE * 2).size(buttonSize).right()
            row()
            add(controllerMoveLeft).space(UI_SPACE * 2).size(buttonSize)
            add(controllerMoveRight).space(UI_SPACE * 2).size(buttonSize)
            add().expandX()
            add(controllerRotateLeft).space(UI_SPACE * 2).size(buttonSize)
            add(controllerRotateRight).space(UI_SPACE * 2).size(buttonSize)
        }

        addGameOverlayToHUD(softController)
        showMessage("Fill complete rows", "Stay below the top")

    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun getScore() = state.score

    override fun updateGame(delta: Float) {

        state.timer += delta

        recordInput()
        moveLaterally()
        rotate()
        drop()
        moveDown()

        // TODO: Record high score, show end of game screen.
        if (false /* Ran into obstacle */) {
            endGame()
        }

    }

    private fun recordInput() {
        // TODO: Equivalent of isKeyJustPressed for scene2d buttons.
        state.moveLeft = controllerMoveLeft.isPressed || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
        state.moveRight = controllerMoveRight.isPressed || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
        state.rotateLeft = controllerRotateLeft.isPressed || Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.A)
        state.rotateRight = controllerRotateRight.isPressed || Gdx.input.isKeyJustPressed(Input.Keys.D)
        state.drop = controllerDrop.isPressed || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
    }

    private fun moveLaterally() {
        if (state.moveLeft && !state.moveRight && isLegalMove(state.currentPiece, state.currentX - 1, state.currentY)) {
            state.currentX --
        } else if (state.moveRight && !state.moveLeft && isLegalMove(state.currentPiece, state.currentX + 1, state.currentY)) {
            state.currentX ++
        }
    }

    private fun rotate() {
        val i = state.currentPieceRotations.indexOf(state.currentPiece)
        val n = state.currentPieceRotations.size
        var nextPiece: Tetronimo? = null

        if (state.rotateLeft && !state.rotateRight) {

            nextPiece = state.currentPieceRotations[(i + n - 1) % n] // Add n to make sure we don't go below zero

        } else if (state.rotateRight && !state.rotateLeft) {

            nextPiece = state.currentPieceRotations[(i + 1) % n]

        }

        if (nextPiece != null && isLegalMove(nextPiece, state.currentX, state.currentY)) {
            state.currentPiece = nextPiece
        }
    }

    private fun drop() {
        if (!state.drop) {
            return
        }

        var newY = state.currentY
        while (isLegalMove(state.currentPiece, state.currentX, newY + 1)) {
            newY ++
        }

        storeTetronimoInGrid(state.currentPiece, state.currentX, newY)
        chooseNewTetronimo()
    }

    private fun isLegalMove(tetronimo: Tetronimo, translateX: Int, translateY: Int): Boolean {
        tetronimo.forEachIndexed { pieceY, row ->
            val y = pieceY + translateY

            row.forEachIndexed { pieceX, present ->
                val x = pieceX + translateX

                if (present) {
                    // Would constitute a move off screen, so not okay.
                    if (x < 0 || x >= TetrisGameState.CELLS_WIDE || y >= TetrisGameState.CELLS_HIGH) {
                        return false
                    }

                    // Would constitute moving on top of an existing piece placed in the grid, not okay.
                    if (state.cells[y][x]) {
                        return false
                    }
                }
            }
        }

        return true
    }

    private fun moveDown() {

        if (state.timer < state.nextTimeStep) {
            return
        }

        state.nextTimeStep = state.nextTimeStep + state.timeStep

        if (isLegalMove(state.currentPiece, state.currentX, state.currentY + 1)) {

            state.currentY ++

        } else {

            storeTetronimoInGrid(state.currentPiece, state.currentX, state.currentY)
            chooseNewTetronimo()

        }

    }

    private fun storeTetronimoInGrid(currentPiece: Tetronimo, translateX: Int, translateY: Int) {
        currentPiece.forEachIndexed { y, row ->
            row.forEachIndexed { x, present ->
                if (present) {
                    state.cells[translateY + y][translateX + x] = true
                }
            }
        }
    }

    private fun chooseNewTetronimo() {
        state.currentX = TetrisGameState.CELLS_WIDE / 2 - 1
        state.currentY = 0
        state.currentPieceRotations = Tetronimos.random()
        state.currentPiece = state.currentPieceRotations[0]
    }

    override fun onReceiveDamage(strength: Int) {

    }

    override fun renderGame(camera: OrthographicCamera) {

        val numCellsHigh = state.cells.size
        val numCellsWide = state.cells[0].size

        val cellWidth = viewport.worldWidth / 3 / numCellsWide
        val cellHeight = viewport.worldHeight / numCellsHigh

        val startX = viewport.worldWidth / 3

        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        r.begin(ShapeRenderer.ShapeType.Filled)
        r.color = Color.WHITE

        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, isFull ->
                if (isFull) {
                    r.rect(startX + x * cellWidth, viewport.worldHeight - (y * cellHeight), cellWidth, cellHeight)
                }
            }
        }
        state.currentPiece.forEachIndexed { y, row ->
            row.forEachIndexed { x, present ->
                if (present) {
                    r.rect(
                        startX + (state.currentX + x) * cellWidth,
                        viewport.worldHeight - (state.currentY + y) * cellHeight,
                        cellWidth,
                        cellHeight
                    )
                }
            }
        }

        r.end()

        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = Color.DARK_GRAY

        // For debugging, it can help to draw every cell:
        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                r.rect(startX + x * cellWidth, y * cellHeight, cellWidth, cellHeight)
            }
        }

        // Draw piece
        r.end()
    }

}
