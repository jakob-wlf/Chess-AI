package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class Bishop(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♗" else "♝"
    }

    override fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>, board: Array<Array<Piece?>>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): Boolean {
        val (fromRow, fromCol) = from
        val (toRow, toCol) = to

        if(!Chessboard.isWithinBounds(to)) return false
        if(fromRow == toRow && fromCol == toCol) return false

        val chessBoardInstance = Chessboard.instance?: return false
        val gameManager = GameManager.instance?: return false
        if(!ignoreKingSafety && chessBoardInstance.canKingBeCapturedAfterMove(from, to, gameManager.gameState)) {
            return false
        }

        val rowDiff = toRow - fromRow
        val colDiff = toCol - fromCol

        // Check for diagonal movement
        if(abs(rowDiff) != abs(colDiff)) {
            return false
        }

        val stepRow = if(rowDiff > 0) 1 else -1
        val stepCol = if(colDiff > 0) 1 else -1
        var currentRow = fromRow + stepRow
        var currentCol = fromCol + stepCol
        while(currentRow != toRow && currentCol != toCol) {
            if(Chessboard.isWithinBounds(Pair(currentRow, currentCol)) && board[currentRow][currentCol] != null) {
                return false // Path is blocked
            }
            currentRow += stepRow
            currentCol += stepCol
        }

        val targetPiece = board[toRow][toCol]
        if(targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        return true
    }

    override fun type(): String {
        return "Bishop"
    }
}