package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class Bishop(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♗" else "♝"
    }

    override fun isValidMove(from: Int, to: Int, board: Array<Piece?>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): Boolean {
        if (!Chessboard.isWithinBounds(to)) return false
        if (from == to) return false

        val chessBoardInstance = Chessboard.instance ?: return false
        val gameManager = GameManager.instance ?: return false
        if (!ignoreKingSafety && chessBoardInstance.canKingBeCapturedAfterMove(from, to, gameManager.gameState)) {
            return false
        }

        val fromRow = from / 8
        val fromCol = from % 8
        val toRow = to / 8
        val toCol = to % 8

        val rowDiff = toRow - fromRow
        val colDiff = toCol - fromCol

        // Check for diagonal movement
        if (abs(rowDiff) != abs(colDiff)) {
            return false
        }

        val stepRow = if (rowDiff > 0) 1 else -1
        val stepCol = if (colDiff > 0) 1 else -1
        var currentRow = fromRow + stepRow
        var currentCol = fromCol + stepCol
        while (currentRow != toRow && currentCol != toCol) {
            val currentPos = currentRow * 8 + currentCol
            if (Chessboard.isWithinBounds(currentPos) && board[currentPos] != null) {
                return false // Path is blocked
            }
            currentRow += stepRow
            currentCol += stepCol
        }

        val targetPiece = board[to]
        if (targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        return true
    }

    override fun type(): String {
        return "Bishop"
    }
}