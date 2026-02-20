package piece

import Chessboard
import java.awt.Color

class Rook(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♖" else "♜"
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

        // Cannot move diagonally
        if (fromRow != toRow && fromCol != toCol) {
            return false
        }

        val targetPiece = board[to]
        if (targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        if (fromRow != toRow) {
            val direction = if (toRow - fromRow > 0) 1 else -1
            var currentRow = fromRow + direction
            while (currentRow != toRow) {
                if (board[currentRow * 8 + toCol] != null) {
                    return false
                }
                currentRow += direction
            }
        } else {
            val direction = if (toCol - fromCol > 0) 1 else -1
            var currentCol = fromCol + direction
            while (currentCol != toCol) {
                if (board[toRow * 8 + currentCol] != null) {
                    return false
                }
                currentCol += direction
            }
        }

        return true
    }

    override fun type(): String {
        return "Rook"
    }
}