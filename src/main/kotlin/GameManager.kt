import piece.Piece
import java.awt.Color

class GameManager(gameId: String? = null, isLichessGame: Boolean = false, color: Color? = null) {

    var gameState: GameState = GameState(
        Array(Chessboard.BOARD_SIZE * Chessboard.BOARD_SIZE) { null }
    )

    init {
        instance = this
        currentGameId = gameId

        if(isLichessGame) {
            gameState = GameState(
                chessBoard = Array(Chessboard.BOARD_SIZE * Chessboard.BOARD_SIZE) { null },
                onlyOnePlayer = false,
                lichessBotColor = color
            )
        }

        if(gameState.onlyOnePlayer && !isLichessGame) {
            println("Starting a solo game. You are playing as ${if(gameState.isSoloPlayerWhite) "White" else "Black"}.")
            if(!gameState.isSoloPlayerWhite) {
                val thread = Thread {
                    val aiMove = ai.ChessAI.getNextMove(gameState)
                    makeMove(aiMove?.first ?: return@Thread, aiMove.second)
                }
                thread.start()
            }
        }
    }

    fun makeMove(from: Int, to: Int, pieceToPromoteTo: String = "queen") {
        val chessBoard = Chessboard.instance ?: return
        chessBoard.makeMove(from, to, gameState, ignoreKingSafety = false, updateUI = true, printErrors = true, pieceToPromoteTo = pieceToPromoteTo)

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
        var currentGameId: String? = null
    }

    data class GameState(val chessBoard: Array<Piece?>,

                         var isWhiteTurn: Boolean = true,
                         val onlyOnePlayer: Boolean = false,
                         val isSoloPlayerWhite: Boolean = false,
                         val lichessBotColor: Color? = null,

                         var whiteKingSideCastlePossible: Boolean = true,
                         var whiteQueenSideCastlePossible: Boolean = true,
                         var blackKingSideCastlePossible: Boolean = true,
                         var blackQueenSideCastlePossible: Boolean = true,

                         var enPassantCaptureFile: Int = -1
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GameState

            if (!chessBoard.contentDeepEquals(other.chessBoard)) return false
            if (isWhiteTurn != other.isWhiteTurn) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chessBoard.contentDeepHashCode()
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
                    if (piece.isValidMove(i, to, this, false)) {
                        possibleMoves.add(i to to)
                    }
                }
            }
            return possibleMoves
        }
    }

}