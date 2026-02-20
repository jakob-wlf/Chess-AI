package piece

import java.awt.Color

class Rook(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♖" else "♜"
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

        // Can not move diagonally
        if(fromRow != toRow && fromCol != toCol)  {
            return false
        }

        val targetPiece = board[toRow][toCol]
        if(targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        if(fromRow != toRow) {
            val direction = if(toRow - fromRow > 0) 1 else -1
            var currentRow = fromRow + direction
            while (currentRow != toRow) {
                if(board[currentRow][toCol] != null) {
                    return false
                }
                currentRow += direction
            }
        }
        else {
            val direction = if(toCol - fromCol > 0) 1 else -1
            var currentCol = fromCol + direction
            while (currentCol != toCol) {
                if(board[toRow][currentCol] != null) {
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