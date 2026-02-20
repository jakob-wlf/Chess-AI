package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class King(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♔" else "♚"
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

        val targetPiece = board[toRow][toCol]
        if(targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        val rowDiff = abs(toRow - fromRow)
        val colDiff = abs(toCol - fromCol)

        if((rowDiff <= 1) && (colDiff <= 1)) {
            return true
        }

        return checkForCastling(from, to, board, moveHistory) && !chessBoardInstance.isInCheck(this.color, board, moveHistory)
    }

    override fun type(): String {
        return "King"
    }

    private fun checkForCastling(from: Pair<Int, Int>, to: Pair<Int, Int>, board: Array<Array<Piece?>>, moveHistory: MutableList<Chessboard.Move>): Boolean {
        if(hasMoved(moveHistory)) return false

        val (fromRow, fromCol) = from
        val (toRow, toCol) = to

        if(fromRow != toRow) return false

        val colDiff = abs(toCol - fromCol)
        if(colDiff != 2) return false

        val kingDirection = if(to.second - from.second > 0) 1 else -1
        val rookColumn = if(kingDirection > 0) 7 else 0
        val rook = board[to.first][rookColumn]?: return false
        if(rook.hasMoved(moveHistory)) return false

        var currentCol = fromCol + kingDirection
        while (currentCol != rookColumn - kingDirection) {
            if(board[fromRow][currentCol] != null) return false
            currentCol += kingDirection
        }

        return true
    }
}