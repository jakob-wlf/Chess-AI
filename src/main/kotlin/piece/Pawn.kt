package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class Pawn(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♙" else "♟"
    }

    override fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>, board: Array<Array<Piece?>>, moveHistory: MutableList<Chessboard.Move>, ignoreKingSafety: Boolean): Boolean {
        val direction = if (color == Color.WHITE) -1 else 1
        val startRow = if (color == Color.WHITE) Chessboard.PAWN_START_ROWS["WHITE"]!! else Chessboard.PAWN_START_ROWS["BLACK"]!!

        val (fromRow, fromCol) = from
        val (toRow, toCol) = to

        if(!Chessboard.isWithinBounds(to)) return false
        if(fromRow == toRow && fromCol == toCol) return false

        val chessBoardInstance = Chessboard.instance?: return false
        val gameManager = GameManager.instance?: return false
        if(!ignoreKingSafety && chessBoardInstance.canKingBeCapturedAfterMove(from, to, gameManager.gameState)) {
            return false
        }

        // Standard move forward
        if(toCol == fromCol) {
            // Move one square forward
            if(toRow == fromRow + direction && board[toRow][toCol] == null) {
                return true
            }
            // Move two squares forward from starting position
            if(fromRow == startRow && toRow == fromRow + 2 * direction && board[fromRow + direction][toCol] == null && board[toRow][toCol] == null) {
                return true
            }

            return false
        }

        // Capture move
        if(toRow == fromRow + direction && (toCol == fromCol + 1 || toCol == fromCol - 1)) {
            val targetPiece = board[toRow][toCol]
            if(targetPiece != null && targetPiece.color != this.color) {
                return true
            }

            //Check for en passant
            return isEnPassantPossible(from, to, board, moveHistory)
        }

        return false
    }

    override fun type(): String {
        return "Pawn"
    }

    private fun isEnPassantPossible(from: Pair<Int, Int>, to: Pair<Int, Int>, board: Array<Array<Piece?>>, moveHistory: MutableList<Chessboard.Move>): Boolean {
        if(board[to.first][to.second] != null) return false
        val (fromRow, _) = from
        val (_, toCol) = to

        val opponentPawn = board[fromRow][toCol] ?: return false
        if(opponentPawn !is Pawn || opponentPawn.color == this.color) return false

        val lastMove = Chessboard.instance?.lastMove(moveHistory) ?: return false
        if(opponentPawn != lastMove.piece) return false
        val (lastFrom, lastTo) = lastMove
        if(abs(lastTo.first - lastFrom.first) != 2) return false

        return true
    }
}