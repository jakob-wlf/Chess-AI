package piece

import Chessboard
import GameManager
import java.awt.Color

abstract class Piece(val color: Color) {
    abstract fun symbol(): String

    fun possibleMoves(position: Int, gameState: GameManager.GameState, ignoreKingSafety: Boolean): List<Int> {
        val moves = mutableListOf<Int>()
        for (to in 0..<(Chessboard.BOARD_SIZE * Chessboard.BOARD_SIZE)) {
            if (isValidMove(position, to, gameState, ignoreKingSafety)) {
                moves.add(to)
            }
        }
        return moves
    }

    abstract fun isValidMove(from: Int, to: Int, gameState: GameManager.GameState, ignoreKingSafety: Boolean): Boolean

    abstract fun type(): String
}