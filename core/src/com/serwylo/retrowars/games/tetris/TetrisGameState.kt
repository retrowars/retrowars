package com.serwylo.retrowars.games.tetris

import com.serwylo.retrowars.games.tetris.entities.Tetronimo
import com.serwylo.retrowars.games.tetris.entities.TetronimoOrientations
import com.serwylo.retrowars.games.tetris.entities.Tetronimos

class TetrisGameState() {

    companion object {

        private val SCORE_PER_LINES = listOf(
            10000,
            20000,
            40000,
            80000
        )

        fun score(numLines: Int) = SCORE_PER_LINES[numLines.coerceAtMost(SCORE_PER_LINES.size - 1)]

        const val CELLS_WIDE = 10
        const val CELLS_HIGH = 20

        /**
         * The amount of seconds between each time step whereby a piece drops one row, for level 1 (0 - 10 lines).
         * @see timeStep
         */
        const val INITIAL_TIME_STEP = 0.75f
        const val MIN_TIME_STEP = 0.75f

        /**
         * Following the original GameBoy tetris speeds.
         * That version runs at ~60 FPS, and the number of frames per time step is available at:
         * https://harddrop.com/wiki/Tetris_(Game_Boy)#Details
         */
        private val TIME_STEPS = listOf(
            53 / 60f,
            49 / 60f,
            45 / 60f,
            41 / 60f,
            37 / 60f,
            33 / 60f,
            28 / 60f,
            22 / 60f,
            17 / 60f,
            11 / 60f,
            10 / 60f,
            9 / 60f,
            8 / 60f,
            7 / 60f,
            6 / 60f,
            6 / 60f,
            5 / 60f,
            5 / 60f,
            4 / 60f,
            4 / 60f,
            3 / 60f,
        )
    }

    fun timeStep() = TIME_STEPS[level().coerceAtMost(TIME_STEPS.size - 1)]
    fun level() = lines / 10

    var nextTimeStep = timeStep()
    var timer = 0f

    var cells: List<MutableList<CellState>> = (0 until CELLS_HIGH).map {
        (0 until CELLS_WIDE).map {
            CellState.Empty
        }.toMutableList()
    }

    var lines: Int = 0

    var currentPieceRotations: TetronimoOrientations = Tetronimos.random()
    var currentPiece: Tetronimo = currentPieceRotations[0]
    var currentX = CELLS_WIDE / 2 - 1
    var currentY = 0

    var nextPieceRotations: TetronimoOrientations = Tetronimos.random()

    var moveLeft = ButtonState.Unpressed
    var moveRight = ButtonState.Unpressed
    var rotateLeft = ButtonState.Unpressed
    var rotateRight = ButtonState.Unpressed
    var drop = ButtonState.Unpressed

}

enum class CellState {
    Empty,
    Full,
    FullFromEnemy,
}

enum class ButtonState {
    Unpressed,
    JustPressed,
    Held,
}