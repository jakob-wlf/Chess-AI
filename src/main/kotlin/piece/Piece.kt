package piece

import Chessboard
import java.awt.Color

abstract class Piece(val color: Color) {
    abstract fun symbol(): String

    fun possibleMoves(position: Int, board: Array<Piece?>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): List<Int> {
        val moves = mutableListOf<Int>()
        for (to in 0..<(Chessboard.BOARD_SIZE * Chessboard.BOARD_SIZE)) {
            if (isValidMove(position, to, board, moveHistory, ignoreKingSafety)) {
                moves.add(to)
            }
        }
        return moves
    }

    abstract fun isValidMove(from: Int, to: Int, board: Array<Piece?>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): Boolean

    abstract fun type(): String

    fun hasMoved(moveHistory: MutableList<Chessboard.Move>): Boolean {
        return moveHistory.any { it.piece == this || it.secondaryPiece == this }
    }
}