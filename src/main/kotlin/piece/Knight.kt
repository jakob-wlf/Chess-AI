package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class Knight(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♘" else "♞"
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

        return (rowDiff == 1 && colDiff == 2) || (rowDiff == 2 && colDiff == 1)
    }

    override fun type(): String {
        return "Knight"
    }
}