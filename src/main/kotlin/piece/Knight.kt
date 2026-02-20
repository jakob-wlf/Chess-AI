package piece

import java.awt.Color
import kotlin.math.abs

class Knight(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♘" else "♞"
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

        if((rowDiff == 1 && colDiff == 2) || (rowDiff == 2 && colDiff == 1)) {
            return true
        }

        return false
    }

    override fun type(): String {
        return "Knight"
    }
}