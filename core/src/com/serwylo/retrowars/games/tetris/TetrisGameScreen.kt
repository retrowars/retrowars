package com.serwylo.retrowars.games.tetris

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.RetrowarsGame
import com.serwylo.retrowars.games.GameScreen
import com.serwylo.retrowars.games.Games
import com.serwylo.retrowars.games.tetris.entities.Tetronimo
import com.serwylo.retrowars.games.tetris.entities.Tetronimos
import com.serwylo.retrowars.ui.IconButton

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

    private val linesLabel = Label("0 lines", game.uiAssets.getStyles().label.large)

    init {

        val skin = game.uiAssets.getSkin()
        val sprites = game.uiAssets.getSprites()

        controllerMoveLeft = IconButton(skin, sprites.buttonIcons.left)
        controllerMoveRight = IconButton(skin, sprites.buttonIcons.right)
        controllerRotateLeft = IconButton(skin, sprites.buttonIcons.rotate_counter_clockwise)
        controllerRotateRight = IconButton(skin, sprites.buttonIcons.rotate_clockwise)
        controllerDrop = IconButton(skin, sprites.buttonIcons.drop)

        val listener = object: ClickListener() {
            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                when (event.listenerActor) {
                    controllerMoveLeft -> state.moveLeft = if (state.moveLeft == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held
                    controllerMoveRight -> state.moveRight = if (state.moveRight == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held
                    controllerRotateLeft -> state.rotateLeft = if (state.rotateLeft == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held
                    controllerRotateRight -> state.rotateRight = if (state.rotateRight == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held
                    controllerDrop -> state.drop = if (state.drop == ButtonState.Unpressed) ButtonState.JustPressed else ButtonState.Held
                }

                return true
            }

            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                when (event.listenerActor) {
                    controllerMoveLeft -> state.moveLeft = ButtonState.Unpressed
                    controllerMoveRight -> state.moveRight = ButtonState.Unpressed
                    controllerRotateLeft -> state.rotateLeft = ButtonState.Unpressed
                    controllerRotateRight -> state.rotateRight = ButtonState.Unpressed
                    controllerDrop -> state.drop = ButtonState.Unpressed
                }
            }
        }

        controllerMoveLeft.addListener(listener)
        controllerMoveRight.addListener(listener)
        controllerRotateLeft.addListener(listener)
        controllerRotateRight.addListener(listener)
        controllerDrop.addListener(listener)

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
        addGameScoreToHUD(linesLabel)
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

        if (!drop()) {
            return
        }

        if (!moveDown()) {
            return
        }

        clearLines()
        resetInput()

    }

    private fun recordInput() {

        // TODO: Holding a key should result in a single action, then a pause, then continued movement.
        //       This is referred to as Delayed Auto Shift (DAS) and is extremely well documented:
        //       https://harddrop.com/wiki/DAS

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            state.moveLeft = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            state.moveRight = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            state.rotateLeft = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            state.rotateRight = ButtonState.JustPressed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            state.drop = ButtonState.JustPressed
        }

    }

    private fun resetInput() {
        state.moveLeft = ButtonState.Unpressed
        state.moveRight = ButtonState.Unpressed
        state.rotateLeft = ButtonState.Unpressed
        state.rotateRight = ButtonState.Unpressed
        state.drop = ButtonState.Unpressed
    }

    private fun moveLaterally() {
        if (state.moveLeft == ButtonState.JustPressed && state.moveRight == ButtonState.Unpressed && isLegalMove(state.currentPiece, state.currentX - 1, state.currentY)) {
            state.currentX --
        } else if (state.moveRight == ButtonState.JustPressed && state.moveLeft == ButtonState.Unpressed && isLegalMove(state.currentPiece, state.currentX + 1, state.currentY)) {
            state.currentX ++
        }
    }

    private fun rotate() {
        val i = state.currentPieceRotations.indexOf(state.currentPiece)
        val n = state.currentPieceRotations.size
        var nextPiece: Tetronimo? = null

        if (state.rotateLeft == ButtonState.JustPressed && state.rotateRight == ButtonState.Unpressed) {

            nextPiece = state.currentPieceRotations[(i + n - 1) % n] // Add n to make sure we don't go below zero

        } else if (state.rotateRight == ButtonState.JustPressed && state.rotateLeft == ButtonState.Unpressed) {

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
        if (state.drop != ButtonState.JustPressed) {
            return true
        }

        var newY = state.currentY + 1
        while (isLegalMove(state.currentPiece, state.currentX, newY + 1)) {
            newY ++
        }

        storeTetronimoInGrid(state.currentPiece, state.currentX, newY)
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
                    if (state.cells[y][x]) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * Return false if the game ends due to the piece moving down, being placed in the grid, and a
     * new tetronimo spawning above the top row.
     */
    private fun moveDown(): Boolean {

        if (state.timer < state.nextTimeStep) {
            return true
        }

        state.nextTimeStep = state.nextTimeStep + state.timeStep()

        if (isLegalMove(state.currentPiece, state.currentX, state.currentY + 1)) {

            state.currentY ++

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
            if (!state.cells[y].all { it }) {
                continue
            }

            numLines ++

            // Make space for the new row to be copied down
            state.cells[y].fill(false)

            // Now copy every cell from this line and above down one row at a time
            for (yToReplace in y downTo 1) {
                for (x in 0 until TetrisGameState.CELLS_WIDE) {
                    state.cells[yToReplace][x] = state.cells[yToReplace - 1][x]
                }
            }

            // And ensure the top row which was moved down is now empty
            state.cells[0].fill(false)

        }

        if (numLines > 0) {
            state.score += TetrisGameState.score(numLines)
            state.lines += numLines
            linesLabel.setText("${state.lines} lines")
        }

    }

    private fun addLine(): Boolean {

        // If the last row has any pieces, it is game over...
        if (state.cells.first().any { it }) {
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
        state.cells.last().fill(true)

        // Second last row will have a hole somewhere (or else it would have been a full line and
        // thus cleared earlier on...
        val indexOfGap = state.cells[TetrisGameState.CELLS_HIGH - 2].indexOf(false)

        // ... so we can align the gap in the new row with this
        state.cells.last()[indexOfGap] = false

        return true

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
        state.currentPieceRotations = state.nextPieceRotations
        state.currentPiece = state.currentPieceRotations[0]

        state.nextPieceRotations = Tetronimos.random()
    }

    override fun onReceiveDamage(strength: Int) {
        for (i in 0 until strength) {
            addLine()
        }
    }

    override fun renderGame(camera: OrthographicCamera) {

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
        r.color = Color.WHITE

        // Draw all full cells first, so they can be overlayed with the grid afterwards.
        state.cells.forEachIndexed { y, row ->
            row.forEachIndexed { x, isFull ->
                if (isFull) {
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
