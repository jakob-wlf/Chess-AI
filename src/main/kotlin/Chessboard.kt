import piece.*
import java.awt.Color
import kotlin.math.abs

class Chessboard(val window: Window) {

    init {
        instance = this
        val gameManager = GameManager.instance
        if(gameManager != null) {
            setupInitialPosition(gameManager.gameState.chessBoard)
            window.renderBoard(gameManager.gameState.chessBoard)
        }
    }

    // TODO: Look into bitboards and also add undo move functionality
    fun makeMove(from: Int, to: Int, gameState: GameManager.GameState, ignoreKingSafety: Boolean = false, updateUI: Boolean = true, printErrors: Boolean = true, allowNullTurn: Boolean = false) {
        val localChessBoard = gameState.chessBoard
        val localMoveHistory = gameState.moveHistory
        val isWhiteTurn = gameState.isWhiteTurn

        val piece = localChessBoard[from]
        if (piece == null) {
            if(printErrors)
                println("No piece at the source position.")
            if(allowNullTurn) {
                gameState.isWhiteTurn = !isWhiteTurn
                if (updateUI)
                    window.renderBoard(localChessBoard)
            }
            return
        }

        if(piece.color == Color.WHITE && !isWhiteTurn ||
           piece.color == Color.BLACK && isWhiteTurn) {
            if(printErrors)
                println("It's not your turn.")
            return
        }

        if (!piece.isValidMove(from, to, localChessBoard, localMoveHistory, ignoreKingSafety)) {
            if(printErrors)
                println("Invalid move for the selected piece.")
            return
        }

        if(isCastling(from, to, piece)) {
            val secondaryPiece = performCastling(localChessBoard, from, to, piece)
            localMoveHistory.add(Move(from, to, piece, secondaryPiece))
        }
        else if(isEnPassant(localChessBoard, from, to, piece)) {
            performEnPassant(localChessBoard, from, to, piece)
            localMoveHistory.add(Move(from, to, piece))
        }
        else {
            localChessBoard[to] = piece
            localChessBoard[from] = null
            if(piece is Pawn && (to < 8 || to > 55)) {
                localChessBoard[to] = Queen(piece.color)
            }
            localMoveHistory.add(Move(from, to, piece))
        }

        gameState.isWhiteTurn = !isWhiteTurn

        if (updateUI)
            window.renderBoard(localChessBoard)
    }

    private fun performEnPassant(board: Array<Piece?>, from: Int, to: Int, piece: Piece) {
        board[from] = null
        board[to] = piece
        board[to + if(piece.color == Color.WHITE) 8 else -8] = null
    }

    private fun isEnPassant(board: Array<Piece?>, from: Int, to: Int, piece: Piece): Boolean {
        if(piece !is Pawn) return false

        // Difference between from and to should be 7 or 9 (diagonal move)
        if(abs(from - to) != 9 && abs(from - to) != 7) return false
        if(board[to] != null) return false;

        return true
    }

    private fun performCastling(board: Array<Piece?>, from: Int, to: Int, piece: Piece): Piece? {
        val kingDirection = if(to - from > 0) 1 else -1
        val rookOffset = if(kingDirection > 0) 3 else -4
        val rook = board[from + rookOffset]!!

        board[from + kingDirection] = rook
        board[from] = null
        board[from + rookOffset] = null
        board[to] = piece

        return rook
    }

    private fun isCastling(from: Int, to: Int, piece: Piece): Boolean {
        if(piece !is King) return false
        if(abs(from - to) != 2) return false
        return true
    }

    private fun setupInitialPosition(chessBoard: Array<Piece?>) {
        for(i in 0..<(BOARD_SIZE * BOARD_SIZE)) {
            if(i in 48..55) {
                chessBoard[i] = Pawn(Color.WHITE)
            } else if(i in 8..15) {
                chessBoard[i] = Pawn(Color.BLACK)
            } else {
                chessBoard[i] = null
            }
        }

        chessBoard[0] = Rook(Color.BLACK)
        chessBoard[7] = Rook(Color.BLACK)
        chessBoard[56] = Rook(Color.WHITE)
        chessBoard[63] = Rook(Color.WHITE)

        chessBoard[4] = King(Color.BLACK)
        chessBoard[60] = King(Color.WHITE)

        chessBoard[1] = Knight(Color.BLACK)
        chessBoard[6] = Knight(Color.BLACK)
        chessBoard[57] = Knight(Color.WHITE)
        chessBoard[62] = Knight(Color.WHITE)

        chessBoard[2] = Bishop(Color.BLACK)
        chessBoard[5] = Bishop(Color.BLACK)
        chessBoard[58] = Bishop(Color.WHITE)
        chessBoard[61] = Bishop(Color.WHITE)

        chessBoard[3] = Queen(Color.BLACK)
        chessBoard[59] = Queen(Color.WHITE)
    }

    fun lastMove(localMoveHistory: MutableList<Move>): Move? {
        return if(localMoveHistory.isNotEmpty()) localMoveHistory.last() else null
    }

    private fun canBeCaptured(position: Int, localChessBoard: Array<Piece?>, localMoveHistory: MutableList<Move>): Boolean {
        val targetPiece = localChessBoard[position] ?: return true
        val color = targetPiece.color
        for(i in 0..<(BOARD_SIZE * BOARD_SIZE)) {
            val piece = localChessBoard[i] ?: continue
            if(piece.color != color) {
                val ignoreKingSafety = targetPiece is King
                if(piece.isValidMove(i, position, localChessBoard, localMoveHistory, ignoreKingSafety)) {
                    return true
                }
            }
        }
        return false
    }

    private fun getKingPosition(color: Color, localChessBoard: Array<Piece?>): Int? {
        for(i in 0..<(BOARD_SIZE * BOARD_SIZE)) {
            val piece = localChessBoard[i] ?: continue
            if(piece is King && piece.color == color) {
                return i
            }
        }
        return null
    }

    fun isInCheck(color: Color, localChessBoard: Array<Piece?>, localMoveHistory: MutableList<Move>): Boolean {
        val kingPosition = getKingPosition(color, localChessBoard) ?: return false
        return canBeCaptured(kingPosition, localChessBoard, localMoveHistory)
    }

    fun canKingBeCapturedAfterMove(from: Int, to: Int, gameState: GameManager.GameState): Boolean {
        val tempBoard = Array(BOARD_SIZE * BOARD_SIZE) { pos -> gameState.chessBoard[pos] }
        val localMoveHistory = gameState.moveHistory.toMutableList()
        val tempState = GameManager.GameState(tempBoard, localMoveHistory, gameState.isWhiteTurn)

        val piece = tempBoard[from] ?: return false
        makeMove(from, to, tempState,  ignoreKingSafety = true, updateUI = false, printErrors = false)

        return isInCheck(piece.color, tempState.chessBoard, tempState.moveHistory)
    }

    fun isDraw(possibleMoves: List<Pair<Int, Int>>, gameState: GameManager.GameState): Boolean {
        val localChessBoard = gameState.chessBoard
        val localMoveHistory = gameState.moveHistory
        val isWhiteTurn = gameState.isWhiteTurn

        return possibleMoves.isEmpty() && !isInCheck(if(isWhiteTurn) Color.WHITE else Color.BLACK, localChessBoard, localMoveHistory)
    }

    fun isCheckmate(possibleMoves: List<Pair<Int, Int>>, gameState: GameManager.GameState): Boolean {
        val localChessBoard = gameState.chessBoard
        val localMoveHistory = gameState.moveHistory
        val isWhiteTurn = gameState.isWhiteTurn

        return possibleMoves.isEmpty() && isInCheck(if(isWhiteTurn) Color.WHITE else Color.BLACK, localChessBoard, localMoveHistory)
    }

    companion object {
        const val BOARD_SIZE: Int = 8

        fun isWithinBounds(position: Int): Boolean {
            return !(position < 0 || position >= (BOARD_SIZE * BOARD_SIZE))
        }

        var instance: Chessboard? = null
    }

    data class Move(val from: Int, val to: Int, val piece: Piece, val secondaryPiece: Piece? = null)


}