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

    fun makeMove(from: Pair<Int, Int>, to: Pair<Int, Int>, gameState: GameManager.GameState, ignoreKingSafety: Boolean = false, updateUI: Boolean = true, printErrors: Boolean = true) {
        val localChessBoard = gameState.chessBoard
        val localMoveHistory = gameState.moveHistory
        val isWhiteTurn = gameState.isWhiteTurn

        val piece = localChessBoard[from.first][from.second]
        if (piece == null) {
            if(printErrors)
                println("No piece at the source position.")
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
            localChessBoard[to.first][to.second] = piece
            localChessBoard[from.first][from.second] = null
            if(piece is Pawn && (to.first == 0 || to.first == BOARD_SIZE - 1)) {
                localChessBoard[to.first][to.second] = Queen(piece.color)
            }
            localMoveHistory.add(Move(from, to, piece))
        }

        gameState.isWhiteTurn = !isWhiteTurn

        if (updateUI)
            window.renderBoard(localChessBoard)
    }

    private fun performEnPassant(board: Array<Array<Piece?>>, from: Pair<Int, Int>, to: Pair<Int, Int>, piece: Piece) {
        board[from.first][to.second] = null
        board[to.first][to.second] = piece
        board[from.first][from.second] = null
    }

    private fun isEnPassant(board: Array<Array<Piece?>>, from: Pair<Int, Int>, to: Pair<Int, Int>, piece: Piece): Boolean {
        if(piece !is Pawn) return false
        if(from.first == to.first) return false
        if(board[to.first][to.second] != null) return false

        return true
    }

    private fun performCastling(board: Array<Array<Piece?>>, from: Pair<Int, Int>, to: Pair<Int, Int>, piece: Piece): Piece? {
        val kingDirection = if(to.second - from.second > 0) 1 else -1
        val rookColumn = if(kingDirection > 0) 7 else 0
        val rook = board[to.first][rookColumn]!!
        board[to.first][rookColumn] = null
        board[to.first][from.second + kingDirection] = rook

        board[from.first][from.second] = null
        board[to.first][to.second] = piece

        return rook
    }

    private fun isCastling(from: Pair<Int, Int>, to: Pair<Int, Int>, piece: Piece): Boolean {
        if(piece !is King) return false
        if(from.first != to.first) return false
        if(abs(from.second - to.second) != 2) return false
        return true
    }

    private fun setupInitialPosition(chessBoard: Array<Array<Piece?>>) {
        for(col in 0..<BOARD_SIZE) {
            chessBoard[PAWN_START_ROWS["WHITE"]!!][col] = Pawn(Color.WHITE)
            chessBoard[PAWN_START_ROWS["BLACK"]!!][col] = Pawn(Color.BLACK)
        }

        chessBoard[0][0] = Rook(Color.BLACK)
        chessBoard[0][7] = Rook(Color.BLACK)
        chessBoard[7][0] = Rook(Color.WHITE)
        chessBoard[7][7] = Rook(Color.WHITE)

        chessBoard[0][4] = King(Color.BLACK)
        chessBoard[7][4] = King(Color.WHITE)

        chessBoard[0][1] = Knight(Color.BLACK)
        chessBoard[0][6] = Knight(Color.BLACK)
        chessBoard[7][1] = Knight(Color.WHITE)
        chessBoard[7][6] = Knight(Color.WHITE)

        chessBoard[0][2] = Bishop(Color.BLACK)
        chessBoard[0][5] = Bishop(Color.BLACK)
        chessBoard[7][2] = Bishop(Color.WHITE)
        chessBoard[7][5] = Bishop(Color.WHITE)

        chessBoard[0][3] = Queen(Color.BLACK)
        chessBoard[7][3] = Queen(Color.WHITE)
    }

    fun lastMove(localMoveHistory: MutableList<Move>): Move? {
        return if(localMoveHistory.isNotEmpty()) localMoveHistory.last() else null
    }

    private fun canBeCaptured(position: Pair<Int, Int>, localChessBoard: Array<Array<Piece?>>, localMoveHistory: MutableList<Move>): Boolean {
        val targetPiece = localChessBoard[position.first][position.second] ?: return true
        val color = targetPiece.color
        for(row in 0..<BOARD_SIZE) {
            for(col in 0..<BOARD_SIZE) {
                val piece = localChessBoard[row][col] ?: continue
                if(piece.color != color) {
                    val ignoreKingSafety = targetPiece is King
                    if(piece.isValidMove(Pair(row, col), position, localChessBoard, localMoveHistory, ignoreKingSafety)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getKingPosition(color: Color, localChessBoard: Array<Array<Piece?>>): Pair<Int, Int>? {
        for(row in 0..<BOARD_SIZE) {
            for(col in 0..<BOARD_SIZE) {
                val piece = localChessBoard[row][col] ?: continue
                if(piece is King && piece.color == color) {
                    return Pair(row, col)
                }
            }
        }
        return null
    }

    fun isInCheck(color: Color, localChessBoard: Array<Array<Piece?>>, localMoveHistory: MutableList<Move>): Boolean {
        val kingPosition = getKingPosition(color, localChessBoard) ?: return false
        return canBeCaptured(kingPosition, localChessBoard, localMoveHistory)
    }

    fun canKingBeCapturedAfterMove(from: Pair<Int, Int>, to: Pair<Int, Int>, gameState: GameManager.GameState): Boolean {
        val tempBoard = Array(BOARD_SIZE) { row -> Array(BOARD_SIZE) { col -> gameState.chessBoard[row][col] } }
        val localMoveHistory = gameState.moveHistory.toMutableList()
        val tempState = GameManager.GameState(tempBoard, localMoveHistory, gameState.isWhiteTurn)

        val piece = tempBoard[from.first][from.second] ?: return false
        makeMove(from, to, tempState,  ignoreKingSafety = true, updateUI = false, printErrors = false)

        return isInCheck(piece.color, tempState.chessBoard, tempState.moveHistory)
    }

    fun isDraw(gameState: GameManager.GameState): Boolean {
        val localChessBoard = gameState.chessBoard
        val localMoveHistory = gameState.moveHistory
        val isWhiteTurn = gameState.isWhiteTurn

        return gameState.getPossibleMoves().isEmpty() && !isInCheck(if(isWhiteTurn) Color.WHITE else Color.BLACK, localChessBoard, localMoveHistory)
    }

    fun isCheckmate(gameState: GameManager.GameState): Boolean {
        val localChessBoard = gameState.chessBoard
        val localMoveHistory = gameState.moveHistory
        val isWhiteTurn = gameState.isWhiteTurn

        return gameState.getPossibleMoves().isEmpty() && isInCheck(if(isWhiteTurn) Color.WHITE else Color.BLACK, localChessBoard, localMoveHistory)
    }

    companion object {
        const val BOARD_SIZE: Int = 8
        val PAWN_START_ROWS = mapOf(
            "WHITE" to 6,
            "BLACK" to 1
        )

        fun isWithinBounds(position: Pair<Int, Int>): Boolean {
            if(position.first < 0 || position.second < 0) return false
            val (row, col) = position
            return row in 0..<BOARD_SIZE && col in 0..<BOARD_SIZE
        }

        var instance: Chessboard? = null
    }

    data class Move(val from: Pair<Int, Int>, val to: Pair<Int, Int>, val piece: Piece, val secondaryPiece: Piece? = null)


}