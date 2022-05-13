package com.serwylo.retrowars.games.tetris

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.tempest.Direction
import com.serwylo.retrowars.games.tetris.entities.Tetronimo
import com.serwylo.retrowars.games.tetris.entities.Tetronimos
import com.serwylo.retrowars.input.TempestSoftController
import com.serwylo.retrowars.input.TetrisSoftController
import com.serwylo.retrowars.ui.ENEMY_ATTACK_COLOUR
import com.serwylo.retrowars.utils.Options

class TetrisGameScreen(game: RetrowarsGame) : GameScreen(game, Games.tetris, 400f, 400f) {

    companion object {
        @Suppress("unused")
        const val TAG = "TetrisGameScreen"
    }

    private val state = TetrisGameState()
    private val sounds = TetrisSoundLibrary()

    private val linesLabel = Label("0 lines", game.uiAssets.getStyles().label.large)

    init {
        addGameScoreToHUD(linesLabel)
    }

    override fun show() {
        Gdx.input.inputProcessor = getInputProcessor()
    }

    override fun updateGame(delta: Float) {
        if (getState() != State.Playing) {
            return
        }

        state.timer += delta

        controller?.update(delta)

        moveLaterally()
        rotate()

        if (!drop()) {
            return
        }

        if (!moveDown()) {
            return
        }

        clearLines()

    }

    private fun moveLaterally() {

        if (controller!!.trigger(TetrisSoftController.Buttons.LEFT) && isLegalMove(state.currentPiece, state.currentX - 1, state.currentY)) {
            state.currentX --
        }

        if (controller.trigger(TetrisSoftController.Buttons.RIGHT) && isLegalMove(state.currentPiece, state.currentX + 1, state.currentY)) {
            state.currentX ++
        }

    }

    private fun rotate() {
        val i = state.currentPieceRotations.indexOf(state.currentPiece)
        val n = state.currentPieceRotations.size
        var nextPiece: Tetronimo? = null

        if (controller!!.trigger(TetrisSoftController.Buttons.ROTATE_CCW)) {
            nextPiece = state.currentPieceRotations[(i + n - 1) % n] // Add n to make sure we don't go below zero
        } else if (controller.trigger(TetrisSoftController.Buttons.ROTATE_CW)) {
            nextPiece = state.currentPieceRotations[(i + 1) % n]
        }

        if (nextPiece != null && isLegalMove(nextPiece, state.currentX, state.currentY)) {
            state.currentPiece = nextPiece
        }
    }

    /**
     * Return false if the game has ended due to this piece dropping and a new piece spawning.
     */
    private fun drop(): Boolean {
        if (!controller!!.trigger(TetrisSoftController.Buttons.DROP)) {
            return true
        }

        var newY = state.currentY + 1
        while (isLegalMove(state.currentPiece, state.currentX, newY)) {
            newY ++
        }

        storeTetronimoInGrid(state.currentPiece, state.currentX, newY - 1)
        chooseNewTetronimo()

        if (!isLegalMove(state.currentPiece, state.currentX, state.currentY)) {
            endGame()
            return false
        }

        return true
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
                    if (state.cells[y][x] != CellState.Empty) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * We move down either because the player is holding the "down" button, or because a certain
     * time has passed and gravitiy is ready to pull it down.
     *
     * Return false if the game ends due to the piece moving down, being placed in the grid, and a
     * new tetronimo spawning above the top row.
     */
    private fun moveDown(): Boolean {
        val isPressingDown = controller!!.trigger(TetrisSoftController.Buttons.DOWN)
        if (!isPressingDown && state.timer < state.nextTimeStep) {
            return true
        }

        if (!isPressingDown && state.timer >= state.nextTimeStep) {
            state.nextTimeStep = state.nextTimeStep + state.timeStep()
        }

        if (isLegalMove(state.currentPiece, state.currentX, state.currentY + 1)) {

            state.currentY ++
            sounds.tick()

        } else {

            storeTetronimoInGrid(state.currentPiece, state.currentX, state.currentY)
            chooseNewTetronimo()

            if (!isLegalMove(state.currentPiece, state.currentX, state.currentY)) {
                endGame()
                return false
            }

        }

        return true

    }

    private fun clearLines() {

        var numLines = 0

        for (y in 0 until TetrisGameState.CELLS_HIGH) {

            // If all values in a row are true then we need to clear the row, otherwise continue.
            if (!state.cells[y].all { it != CellState.Empty }) {
                continue
            }

            numLines ++

            // Make space for the new row to be copied down
            state.cells[y].fill(CellState.Empty)

            // Now copy every cell from this line and above down one row at a time
            for (yToReplace in y downTo 1) {
                for (x in 0 until TetrisGameState.CELLS_WIDE) {
                    state.cells[yToReplace][x] = state.cells[yToReplace - 1][x]
                }
            }

            // And ensure the top row which was moved down is now empty
            state.cells[0].fill(CellState.Empty)

        }

        if (numLines > 0) {
            increaseScore(TetrisGameState.score(numLines))
            state.lines += numLines
            linesLabel.setText("${state.lines} lines")
        }

    }

    private fun addLine(): Boolean {

        // If the last row has any pieces, it is game over...
        if (state.cells.first().any { it != CellState.Empty }) {
            endGame()
            return false
        }

        for (y in 1 until TetrisGameState.CELLS_HIGH) {

            // Copy all cells up one row.
            for (x in 0 until TetrisGameState.CELLS_WIDE) {
                state.cells[y - 1][x] = state.cells[y][x]
            }

        }

        // Fill the row, but leave one cell free so this line can be cleared...
        state.cells.last().fill(CellState.FullFromEnemy)

        // Second last row will have a hole somewhere (or else it would have been a full line and
        // thus cleared earlier on...
        val indexOfGap = state.cells[TetrisGameState.CELLS_HIGH - 2].indexOf(CellState.Empty)

        // ... so we can align the gap in the new row with this
        state.cells.last()[indexOfGap] = CellState.Empty

        return true

    }

    private fun storeTetronimoInGrid(currentPiece: Tetronimo, translateX: Int, translateY: Int) {
        currentPiece.forEachIndexed { y, row ->
            row.forEachIndexed { x, present ->
                if (present) {
                    state.cells[translateY + y][translateX + x] = CellState.Full
                }
            }
        }
    }

    private fun chooseNewTetronimo() {
        state.currentX = TetrisGameState.CELLS_WIDE / 2 - 1
        state.currentY = 0
        state.currentPieceRotations = state.nextPieceRotations
        state.currentPiece = state.currentPieceRotations[0]

        state.nextPieceRotations = Tetronimos.random()
    }

    override fun onReceiveDamage(strength: Int) {
        for (i in 0 until strength) {
            addLine()
        }
    }

    override fun renderGame(camera: Camera) {

        val numCellsHigh = state.cells.size
        val numCellsWide = state.cells[0].size

        // Leave one cell worth of space at the top and the bottom, hence numCellsHigh + 2.
        val cellHeight = viewport.worldHeight / (numCellsHigh + 2)
        val cellWidth = cellHeight

        val startX = (viewport.worldWidth - (cellWidth * numCellsWide)) / 2
        val startY = cellHeight

        val r = game.uiAssets.shapeRenderer
        r.projectionMatrix = camera.combined

        r.begin(ShapeRenderer.ShapeType.Filled)

        // Draw all full cells first, so they can be overlayed with the grid afterwards.
        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, cellState ->
                if (cellState != CellState.Empty) {
                    r.color = if (cellState == CellState.FullFromEnemy) ENEMY_ATTACK_COLOUR else Color.WHITE
                    r.rect(
                        startX + (x * cellWidth),
                        viewport.worldHeight - startY - ((y + 1) * cellHeight),
                        cellWidth,
                        cellHeight
                    )
                }
            }
        }

        // Then draw the current tetronimo (again, so it can be overlayed with the grid afterwards).
        r.color = Color.WHITE
        state.currentPiece.forEachIndexed { y, row ->
            row.forEachIndexed { x, present ->
                if (present) {
                    r.rect(
                        startX + ((state.currentX + x) * cellWidth),
                        viewport.worldHeight - startY - ((state.currentY + y + 1) * cellHeight),
                        cellWidth,
                        cellHeight
                    )
                }
            }
        }

        // Off to the side of the grid draw the next tetronimo
        state.nextPieceRotations[0].forEachIndexed { y, row ->
            row.forEachIndexed { x, present ->
                if (present) {
                    r.rect(
                        startX + (numCellsWide * cellWidth) + (cellWidth * 2) + (x * cellWidth),
                        viewport.worldHeight - startY - (cellHeight) - (y * cellHeight),
                        cellWidth,
                        cellHeight
                    )
                }
            }
        }

        r.end()

        // Draw cell borders over the top of tetronimos and also over the entire grid (including empty cells).
        r.begin(ShapeRenderer.ShapeType.Line)
        r.color = Color.DARK_GRAY

        state.cells.forEachIndexed { y, row ->
            for (x in 0 until row.size) {
                r.rect(
                    startX + x * cellWidth,
                    viewport.worldHeight - startY - ((y + 1) * cellHeight),
                    cellWidth,
                    cellHeight
                )
            }
        }

        state.nextPieceRotations[0].forEachIndexed { y, row ->
            row.forEachIndexed { x, present ->
                if (present) {
                    r.rect(
                        startX + (numCellsWide * cellWidth) + (cellWidth * 2) + (x * cellWidth),
                        viewport.worldHeight - startY - (cellHeight) - (y * cellHeight),
                        cellWidth,
                        cellHeight
                    )
                }
            }
        }

        r.end()
    }

}
