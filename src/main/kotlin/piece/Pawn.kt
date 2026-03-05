package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class Pawn(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♙" else "♟"
    }

    override fun isValidMove(from: Int, to: Int, gameState: GameManager.GameState, ignoreKingSafety: Boolean): Boolean {
        val direction = if (color == Color.WHITE) -1 else 1
        val startRow = if (color == Color.WHITE) 6 else 1

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

        // Standard move forward
        if (toCol == fromCol) {
            // Move one square forward
            if (toRow == fromRow + direction && gameState.chessBoard[to] == null) {
                return true
            }
            // Move two squares forward from starting position
            if (fromRow == startRow && toRow == fromRow + 2 * direction
                && gameState.chessBoard[(fromRow + direction) * 8 + toCol] == null
                && gameState.chessBoard[to] == null
            ) {
                return true
            }

            return false
        }

        // Capture move
        if (toRow == fromRow + direction && (toCol == fromCol + 1 || toCol == fromCol - 1)) {
            val targetPiece = gameState.chessBoard[to]
            if (targetPiece != null && targetPiece.color != this.color) {
                return true
            }

            // Check for en passant
            return isEnPassantPossible(from, to, gameState)
        }

        return false
    }

    override fun type(): String {
        return "Pawn"
    }

    private fun isEnPassantPossible(from: Int, to: Int, gameState: GameManager.GameState): Boolean {
        if (gameState.chessBoard[to] != null) return false

        val fromRow = from / 8
        val toCol = to % 8

        val opponentPawn = gameState.chessBoard[fromRow * 8 + toCol] ?: return false
        if (opponentPawn !is Pawn || opponentPawn.color == this.color) return false

        if(gameState.enPassantCaptureFile == -1 || gameState.enPassantCaptureFile != toCol) return false

        return true
    }
}