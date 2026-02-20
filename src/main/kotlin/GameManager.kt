import piece.Piece
import java.awt.Color

class GameManager {

    val gameState: GameState = GameState(
        Array(Chessboard.BOARD_SIZE * Chessboard.BOARD_SIZE) { null },
        mutableListOf(),
        true
    )

    init {
        instance = this
    }

    fun makeMove(from: Int, to: Int) {
        val chessBoard = Chessboard.instance ?: return
        chessBoard.makeMove(from, to, gameState, ignoreKingSafety = false, updateUI = true, printErrors = true)

        println("Current evaluation: ${ai.ChessAI.evaluate(gameState, gameState.getPossibleMoves())}")
        val possibleMoves: List<Pair<Int, Int>> = gameState.getPossibleMoves()

        if(chessBoard.isCheckmate(possibleMoves, gameState)) {
            println("Checkmate! ${if(gameState.isWhiteTurn) "Black" else "White"} wins!")
            return
        }
        else if(chessBoard.isDraw(possibleMoves, gameState)) {
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

    data class GameState(val chessBoard: Array<Piece?>, val moveHistory: MutableList<Chessboard.Move>, var isWhiteTurn: Boolean, val onlyOnePlayer: Boolean = true, val isSoloPlayerWhite: Boolean = false) {
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

        fun getPossibleMoves(): List<Pair<Int, Int>> {
            val possibleMoves = mutableListOf<Pair<Int, Int>>()
            val currentColor = if (isWhiteTurn) Color.WHITE else Color.BLACK
            for(i in chessBoard.indices) {
                val piece = chessBoard[i] ?: continue
                if((piece.color != currentColor)) continue

                for (to in chessBoard.indices) {
                    if (piece.isValidMove(i, to, chessBoard, moveHistory, false)) {
                        possibleMoves.add(i to to)
                    }
                }
            }
            return possibleMoves
        }
    }

}