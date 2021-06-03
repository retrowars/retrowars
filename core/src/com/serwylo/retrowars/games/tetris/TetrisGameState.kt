package com.serwylo.retrowars.games.tetris

import com.serwylo.retrowars.games.tetris.entities.Tetronimo
import com.serwylo.retrowars.games.tetris.entities.TetronimoOrientations
import com.serwylo.retrowars.games.tetris.entities.Tetronimos

class TetrisGameState() {

    companion object {
        const val CELLS_WIDE = 10
        const val CELLS_HIGH = 20
    }

    var timeStep = 0.75f
    var nextTimeStep = timeStep
    var timer = 0f

    var cells: List<MutableList<Boolean>>
    var score: Long = 0

    var currentPieceRotations: TetronimoOrientations = Tetronimos.random()
    var currentPiece: Tetronimo = currentPieceRotations[0]
    var currentX = CELLS_WIDE / 2 - 1
    var currentY = 0

    var moveLeft = false
    var moveRight = false
    var rotateLeft = false
    var rotateRight = false
    var drop = false

    init {
        cells = (0 until CELLS_HIGH).map { _ ->
            (0 until CELLS_WIDE).map { _ ->
                false
            }.toMutableList()
        }
    }

    class Cell(val x: Int, val y: Int) {
        override fun toString() = "($x, $y)"
    }

}
