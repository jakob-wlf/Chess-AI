package ai

import Chessboard.Companion.BOARD_SIZE
import GameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Color

class ChessAI {

    companion object {
        private const val searchDepth = 3

        private val pieceValues = mapOf( // In centipawns
            "Pawn" to 100,
            "Knight" to 320,
            "Bishop" to 330,
            "Rook" to 500,
            "Queen" to 900,
            "King" to 0
        )

        fun getNextMove(gameState: GameManager.GameState): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
            println("AI is thinking...")
            val chessBoard = Chessboard.instance ?: return null
            val possibleMoves = gameState.getPossibleMoves()

            // Evaluate each root move in parallel
            val results: List<Pair<Pair<Pair<Int, Int>, Pair<Int, Int>>, Int>> = runBlocking {
                possibleMoves.map { move ->
                    async(Dispatchers.Default) {

                        // Clone State to not affect the actual game state while simulating moves
                        val tempBoard = Array(BOARD_SIZE) { gameState.chessBoard[it].copyOf() }
                        val tempState = GameManager.GameState(tempBoard, mutableListOf(), gameState.isWhiteTurn)

                        chessBoard.makeMove(move.first, move.second, tempState, ignoreKingSafety = true, updateUI = false, printErrors = false)
                        val score = alphabeta(tempState, searchDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, maximizing = !gameState.isWhiteTurn)

                        move to score
                    }
                }.awaitAll()
            }

            // Pick best result after all threads complete
            return if (gameState.isWhiteTurn) {
                results.maxByOrNull { it.second }?.first
            } else {
                results.minByOrNull { it.second }?.first
            }
        }

        private fun alphabeta(gameState: GameManager.GameState, depth: Int, alpha: Int, beta: Int, maximizing: Boolean): Int {
            var a = alpha
            var b = beta

            val possibleMoves = gameState.getPossibleMoves()

            if(depth == 0 || Chessboard.instance?.isCheckmate(gameState) == true || Chessboard.instance?.isDraw(gameState) == true) {
                return evaluate(gameState, possibleMoves)
            }

            if(maximizing) {
                var value = Int.MIN_VALUE
                for (move in possibleMoves) {

                    // Clone State to not affect the actual game state while simulating moves
                    val tempBoard = Array(BOARD_SIZE) { gameState.chessBoard[it].copyOf() }
                    val tempState = GameManager.GameState(tempBoard, mutableListOf(), gameState.isWhiteTurn)

                    Chessboard.instance?.makeMove(move.first, move.second, tempState, ignoreKingSafety = true, updateUI = false, printErrors = false)
                    value = maxOf(value, alphabeta(tempState, depth - 1, alpha, beta, maximizing = false))

                    a = maxOf(a, value)
                    if (a >= b) {
                        break // Beta cut-off
                    }
                }

                return value
            }
            else {
                var value = Int.MAX_VALUE
                for (move in possibleMoves) {

                    // Clone State to not affect the actual game state while simulating moves
                    val tempBoard = Array(BOARD_SIZE) { gameState.chessBoard[it].copyOf() }
                    val tempState = GameManager.GameState(tempBoard, mutableListOf(), gameState.isWhiteTurn)

                    Chessboard.instance?.makeMove(move.first, move.second, tempState, ignoreKingSafety = true, updateUI = false, printErrors = false)
                    value = minOf(value, alphabeta(tempState, depth - 1, alpha, beta, maximizing = true))

                    b = minOf(b, value)
                    if (a >= b) {
                        break // Beta cut-off
                    }
                }

                return value
            }
        }

        fun evaluate(
            gameState: GameManager.GameState,
            preComputedPossibleMoves: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>? = null
        ): Int {
            var score = 0

            score += materialValue(gameState)
            score += pieceSquareTables(gameState)
            score += mobility(gameState, preComputedPossibleMoves)
            score += pawnStructure(gameState)

            return score
        }

        private fun pawnStructure(gameState: GameManager.GameState): Int {
            var score = 0
            score += evaluatePawnsOfColor(gameState, Color.WHITE)
            score -= evaluatePawnsOfColor(gameState, Color.BLACK)

            return score
        }

        private fun evaluatePawnsOfColor(gameState: GameManager.GameState, white: Color?): Int {
            var score = 0
            val allPawns = mutableListOf<Pair<Int, Int>>()
            for ((rowIndex, row) in gameState.chessBoard.withIndex()) {
                for ((colIndex, piece) in row.withIndex()) {
                    if (piece != null && piece.type() == "Pawn" && piece.color == white) {
                        allPawns.add(Pair(rowIndex, colIndex))
                    }
                }
            }

            // Doubled pawns
            for (col in 0..<BOARD_SIZE) {
                val pawnsInColumn = allPawns.count { it.second == col }
                if (pawnsInColumn > 1) {
                    score -= 25 * (pawnsInColumn - 1)
                }
            }

            // Isolated pawns
            for (pawn in allPawns) {
                val col = pawn.second
                val hasLeftSupport = allPawns.any { it.second == col - 1 }
                val hasRightSupport = allPawns.any { it.second == col + 1 }
                if (!hasLeftSupport && !hasRightSupport) {
                    score -= 30
                }
            }

            // Backward pawns
            for (pawn in allPawns) {
                val row = pawn.first
                val col = pawn.second
                val direction = if (white == Color.WHITE) -1 else 1
                val hasFriendlyPawnAhead = allPawns.any { it.first == row + direction && it.second == col }
                if (!hasFriendlyPawnAhead) {
                    score -= 25
                }
            }

            // Passed pawns
            for (pawn in allPawns) {
                val row = pawn.first
                val col = pawn.second
                val direction = if (white == Color.WHITE) -1 else 1
                val hasEnemyPawnAhead = gameState.chessBoard.withIndex().any { (r, rowPieces) ->
                    rowPieces.withIndex().any { (c, piece) ->
                        piece != null && piece.type() == "Pawn" && piece.color != white &&
                                c == col && ((direction == -1 && r < row) || (direction == 1 && r > row))
                    }
                }
                if (!hasEnemyPawnAhead) {
                    val rank = if (white == Color.WHITE) 7 - row else row
                    score += 20 * rank
                }
            }

            // Chain pawns
            for (pawn in allPawns) {
                val row = pawn.first
                val col = pawn.second
                val direction = if (white == Color.WHITE) -1 else 1
                val hasFriendlyPawnDiagonal = gameState.chessBoard.withIndex().any { (r, rowPieces) ->
                    rowPieces.withIndex().any { (c, piece) ->
                        piece != null && piece.type() == "Pawn" && piece.color == white &&
                                ((c == col - 1 || c == col + 1) && r == row + direction)
                    }
                }
                if (hasFriendlyPawnDiagonal) {
                    score += 15
                }
            }

            return score
        }

        private fun mobility(
            gameState: GameManager.GameState,
            preComputedPossibleMoves: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>?
        ): Int {
            preComputedPossibleMoves?: return 0

            val whiteMoveCount = preComputedPossibleMoves.count { gameState.chessBoard[it.first.first][it.first.second]?.color == Color.WHITE }

            val blackMoveCount = preComputedPossibleMoves.size - whiteMoveCount

            return 5 * (whiteMoveCount - blackMoveCount)
        }

        private fun pieceSquareTables(gameState: GameManager.GameState): Int {
            var value = 0
            for ((rowIndex, row) in gameState.chessBoard.withIndex()) {
                for ((colIndex, piece) in row.withIndex()) {
                    if (piece != null) {
                        val pieceValue = when (piece.type()) {
                            "Pawn"   -> PieceSquareTables.getPawnValue(rowIndex, colIndex, piece.color == Color.WHITE)
                            "Knight" -> PieceSquareTables.getKnightValue(rowIndex, colIndex, piece.color == Color.WHITE)
                            "Bishop" -> PieceSquareTables.getBishopValue(rowIndex, colIndex, piece.color == Color.WHITE)
                            "Rook"   -> PieceSquareTables.getRookValue(rowIndex, colIndex, piece.color == Color.WHITE)
                            "Queen"  -> PieceSquareTables.getQueenValue(rowIndex, colIndex, piece.color == Color.WHITE)
                            else -> 0
                        }
                        value += if (piece.color == Color.WHITE) pieceValue else -pieceValue
                    }
                }
            }
            return value
        }

        private fun materialValue(gameState: GameManager.GameState): Int {
            var value = 0
            for(row in gameState.chessBoard) {
                for(piece in row) {
                    if(piece != null) {
                        val pieceValue = pieceValues[piece.type()] ?: 0
                        value += if(piece.color == Color.WHITE) pieceValue else -pieceValue
                    }
                }
            }
            return value
        }
    }
}