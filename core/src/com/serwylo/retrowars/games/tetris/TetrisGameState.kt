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
    }

    var timeStep = 0.75f
    var nextTimeStep = timeStep
    var timer = 0f

    var cells: List<MutableList<Boolean>>
    var score: Long = 0
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

    init {
        cells = (0 until CELLS_HIGH).map { _ ->
            (0 until CELLS_WIDE).map { _ ->
                false
            }.toMutableList()
        }
    }

}

enum class ButtonState {
    Unpressed,
    JustPressed,
    Held,
}
