package piece

import Chessboard
import java.awt.Color
import kotlin.math.abs

class King(color: Color) : Piece(color) {
    override fun symbol(): String {
        return if (color == Color.WHITE) "♔" else "♚"
    }

    override fun isValidMove(from: Int, to: Int, gameState: GameManager.GameState, ignoreKingSafety: Boolean): Boolean {
        if (!Chessboard.isWithinBounds(to)) return false
        if (from == to) return false

        val chessBoardInstance = Chessboard.instance ?: return false
        val gameManager = GameManager.instance ?: return false
        if (!ignoreKingSafety && chessBoardInstance.canKingBeCapturedAfterMove(from, to, gameManager.gameState)) {
            return false
        }

        val targetPiece = gameState.chessBoard[to]
        if (targetPiece != null && targetPiece.color == this.color) {
            return false
        }

        val fromRow = from / 8
        val fromCol = from % 8
        val toRow = to / 8
        val toCol = to % 8

        val rowDiff = abs(toRow - fromRow)
        val colDiff = abs(toCol - fromCol)

        if ((rowDiff <= 1) && (colDiff <= 1)) {
            return true
        }

        return checkForCastling(from, to, gameState) && !chessBoardInstance.isInCheck(this.color, gameState)
    }

    override fun type(): String {
        return "King"
    }

    private fun checkForCastling(from: Int, to: Int, gameState: GameManager.GameState): Boolean {
        if (color == Color.WHITE &&
            !gameState.whiteKingSideCastlePossible &&
            !gameState.whiteQueenSideCastlePossible) return false

        val fromRow = from / 8
        val fromCol = from % 8
        val toRow = to / 8
        val toCol = to % 8

        if (fromRow != toRow) return false

        val colDiff = abs(toCol - fromCol)
        if (colDiff != 2) return false

        val kingDirection = if (toCol - fromCol > 0) 1 else -1
        val rookColumn = if (kingDirection > 0) 7 else 0

        // Check if Rook has moved
        val isKingSide = (rookColumn == 7)
        if(color == Color.WHITE) {
            if((isKingSide && !gameState.whiteKingSideCastlePossible) ||
                (!isKingSide && !gameState.whiteQueenSideCastlePossible))
                return false
        } else {
            if((isKingSide && !gameState.blackKingSideCastlePossible) ||
                (!isKingSide && !gameState.blackQueenSideCastlePossible))
                return false
        }

        // Check if rook is till in place for safety bc Idfk anymore
        val rookOffset = if(kingDirection > 0) 3 else -4
        gameState.chessBoard[from + rookOffset]?: return false

        var currentCol = fromCol + kingDirection
        while (currentCol != rookColumn) {
            if (gameState.chessBoard[fromRow * 8 + currentCol] != null) return false
            currentCol += kingDirection
        }

        return true
    }
}