package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class King(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♔" else "♚"
    }

    override fun isValidMove(from: Int, to: Int, board: Array<Piece?>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): Boolean {
        if (!Chessboard.isWithinBounds(to)) return false
        if (from == to) return false

        val chessBoardInstance = Chessboard.instance ?: return false
        val gameManager = GameManager.instance ?: return false
        if (!ignoreKingSafety && chessBoardInstance.canKingBeCapturedAfterMove(from, to, gameManager.gameState)) {
            return false
        }

        val targetPiece = board[to]
        if (targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        val fromRow = from / 8
        val fromCol = from % 8
        val toRow = to / 8
        val toCol = to % 8

        val rowDiff = abs(toRow - fromRow)
        val colDiff = abs(toCol - fromCol)

        if ((rowDiff <= 1) && (colDiff <= 1)) {
            return true
        }

        return checkForCastling(from, to, board, moveHistory) && !chessBoardInstance.isInCheck(this.color, board, moveHistory)
    }

    override fun type(): String {
        return "King"
    }

    private fun checkForCastling(from: Int, to: Int, board: Array<Piece?>, moveHistory: MutableList<Chessboard.Move>): Boolean {
        if (hasMoved(moveHistory)) return false

        val fromRow = from / 8
        val fromCol = from % 8
        val toRow = to / 8
        val toCol = to % 8

        if (fromRow != toRow) return false

        val colDiff = abs(toCol - fromCol)
        if (colDiff != 2) return false

        val kingDirection = if (toCol - fromCol > 0) 1 else -1
        val rookColumn = if (kingDirection > 0) 7 else 0
        val rook = board[fromRow * 8 + rookColumn] ?: return false
        if (rook.hasMoved(moveHistory)) return false

        var currentCol = fromCol + kingDirection
        while (currentCol != rookColumn - kingDirection) {
            if (board[fromRow * 8 + currentCol] != null) return false
            currentCol += kingDirection
        }

        return true
    }
}