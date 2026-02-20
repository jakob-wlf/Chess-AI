import piece.Piece
import java.awt.Color

class GameManager {

    val gameState: GameState = GameState(
        Array(Chessboard.BOARD_SIZE) { Array(Chessboard.BOARD_SIZE) { null } },
        mutableListOf(),
        true
    )

    init {
        instance = this
    }

    fun makeMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val chessBoard = Chessboard.instance ?: return
        chessBoard.makeMove(from, to, gameState, ignoreKingSafety = false, updateUI = true, printErrors = true)

        println("Current evaluation: ${ai.ChessAI.evaluate(gameState, gameState.getPossibleMoves())}")

        if(chessBoard.isCheckmate(gameState)) {
            println("Checkmate! ${if(gameState.isWhiteTurn) "Black" else "White"} wins!")
            return
        }
        else if(chessBoard.isDraw(gameState)) {
            println("It's a draw!")
            return
        }

        if(gameState.onlyOnePlayer) {
            if(gameState.isSoloPlayerWhite && !gameState.isWhiteTurn || !gameState.isSoloPlayerWhite && gameState.isWhiteTurn) {
                val thread = Thread {
                    val aiMove = ai.ChessAI.getNextMove(gameState)
                    makeMove(aiMove?.first ?: return@Thread, aiMove.second)
                }
                thread.start()
            }
        }
    }

    companion object {
        var instance: GameManager? = null
    }

    data class GameState(val chessBoard: Array<Array<Piece?>>, val moveHistory: MutableList<Chessboard.Move>, var isWhiteTurn: Boolean, val onlyOnePlayer: Boolean = true, val isSoloPlayerWhite: Boolean = true) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GameState

            if (!chessBoard.contentDeepEquals(other.chessBoard)) return false
            if (moveHistory != other.moveHistory) return false
            if (isWhiteTurn != other.isWhiteTurn) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chessBoard.contentDeepHashCode()
            result = 31 * result + moveHistory.hashCode()
            result = 31 * result + isWhiteTurn.hashCode()
            return result
        }

        fun getPossibleMoves(): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
            val possibleMoves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
            for(row in 0..<Chessboard.BOARD_SIZE) {
                for(col in 0..<Chessboard.BOARD_SIZE) {
                    val piece = chessBoard[row][col] ?: continue
                    if((piece.color == Color.WHITE && !isWhiteTurn) || (piece.color == Color.BLACK && isWhiteTurn)) {
                        continue
                    }
                    for(toRow in 0..<Chessboard.BOARD_SIZE) {
                        for(toCol in 0..<Chessboard.BOARD_SIZE) {
                            if(piece.isValidMove(Pair(row, col), Pair(toRow, toCol), chessBoard, moveHistory, false)) {
                                possibleMoves.add(Pair(Pair(row, col), Pair(toRow, toCol)))
                            }
                        }
                    }
                }
            }
            return possibleMoves
        }
    }

}