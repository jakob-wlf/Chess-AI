package piece

import Chessboard
import java.awt.Color

abstract class Piece(val color: Color) {
    abstract fun symbol(): String

    fun possibleMoves(position: Pair<Int, Int>, board: Array<Array<Piece?>>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        for (row in 0..<Chessboard.BOARD_SIZE) {
            for (col in 0..<Chessboard.BOARD_SIZE) {
                val to = Pair(row, col)
                if (isValidMove(position, to, board, moveHistory, ignoreKingSafety)) {
                    moves.add(to)
                }
            }
        }
        return moves
    }

    abstract fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>, board: Array<Array<Piece?>>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): Boolean

    abstract fun type(): String

    fun hasMoved(moveHistory: MutableList<Chessboard.Move>): Boolean {
        return moveHistory.any { it.piece == this || it.secondaryPiece == this }
    }
}