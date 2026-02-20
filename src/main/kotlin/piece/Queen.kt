package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class Queen(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♕" else "♛"
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

        val rowDiff = toRow - fromRow
        val colDiff = toCol - fromCol

        // Check for diagonal movement or straight movement
        if (abs(rowDiff) != abs(colDiff) && fromRow != toRow && fromCol != toCol) {
            return false
        }

        // Handle Diagonal Movement
        if (abs(rowDiff) == abs(colDiff)) {
            val stepRow = if (rowDiff > 0) 1 else -1
            val stepCol = if (colDiff > 0) 1 else -1
            var currentRow = fromRow + stepRow
            var currentCol = fromCol + stepCol
            while (currentRow != toRow && currentCol != toCol) {
                if (board[currentRow * 8 + currentCol] != null) {
                    return false // Path is blocked
                }
                currentRow += stepRow
                currentCol += stepCol
            }
        }
        // Handle Straight Movement
        else {
            if (fromRow != toRow) {
                val direction = if (rowDiff > 0) 1 else -1
                var currentRow = fromRow + direction
                while (currentRow != toRow) {
                    if (board[currentRow * 8 + toCol] != null) {
                        return false
                    }
                    currentRow += direction
                }
            } else {
                val direction = if (colDiff > 0) 1 else -1
                var currentCol = fromCol + direction
                while (currentCol != toCol) {
                    if (board[toRow * 8 + currentCol] != null) {
                        return false
                    }
                    currentCol += direction
                }
            }
        }

        return true
    }

    override fun type(): String {
        return "Queen"
    }
}