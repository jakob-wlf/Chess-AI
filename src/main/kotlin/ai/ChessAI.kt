package ai

import Chessboard
import Chessboard.Companion.BOARD_SIZE
import GameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Color

class ChessAI {

    companion object {
        private const val searchDepth = 4

        private val pieceValues = mapOf(
            "Pawn" to 100,
            "Knight" to 320,
            "Bishop" to 330,
            "Rook" to 500,
            "Queen" to 900,
            "King" to 0
        )

        fun getNextMove(gameState: GameManager.GameState): Pair<Int, Int>? {
            println("AI is thinking...")
            val chessBoard = Chessboard.instance ?: return null
            val possibleMoves = gameState.getPossibleMoves()

            val results: List<Pair<Pair<Int, Int>, Int>> = runBlocking {
                possibleMoves.map { move ->
                    async(Dispatchers.Default) {
                        val tempBoard = gameState.chessBoard.copyOf()
                        val tempState = GameManager.GameState(tempBoard, gameState.moveHistory.toMutableList(), gameState.isWhiteTurn)

                        chessBoard.makeMove(move.first, move.second, tempState, ignoreKingSafety = true, updateUI = false, printErrors = false)
                        val score = alphabeta(tempState, searchDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, maximizing = !gameState.isWhiteTurn)

                        move to score
                    }
                }.awaitAll()
            }

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

            if (depth == 0 || Chessboard.instance?.isCheckmate(possibleMoves, gameState) == true || Chessboard.instance?.isDraw(possibleMoves, gameState) == true) {
                return evaluate(gameState, possibleMoves) // TODO: Use Transposition Table here to avoid re-evaluating positions we've already seen
            }

            if (maximizing) {
                var value = Int.MIN_VALUE
                for (move in orderMoves(possibleMoves, gameState)) {
                    val tempBoard = gameState.chessBoard.copyOf()
                    val tempState = GameManager.GameState(tempBoard, gameState.moveHistory.toMutableList(), gameState.isWhiteTurn)

                    Chessboard.instance?.makeMove(move.first, move.second, tempState, ignoreKingSafety = false, updateUI = false, printErrors = false)
                    value = maxOf(value, alphabeta(tempState, depth - 1, a, b, maximizing = false))

                    a = maxOf(a, value)
                    if (a >= b) break
                }
                return value
            } else {
                var value = Int.MAX_VALUE
                for (move in orderMoves(possibleMoves, gameState)) {
                    val tempBoard = gameState.chessBoard.copyOf()
                    val tempState = GameManager.GameState(tempBoard, gameState.moveHistory.toMutableList(), gameState.isWhiteTurn)

                    Chessboard.instance?.makeMove(move.first, move.second, tempState, ignoreKingSafety = false, updateUI = false, printErrors = false)
                    value = minOf(value, alphabeta(tempState, depth - 1, a, b, maximizing = true))

                    b = minOf(b, value)
                    if (a >= b) break
                }
                return value
            }
        }

        private fun orderMoves(moves: List<Pair<Int, Int>>, gameState: GameManager.GameState): List<Pair<Int, Int>> {
            return moves.sortedByDescending { move ->
                val targetPiece = gameState.chessBoard[move.second]
                val movingPiece = gameState.chessBoard[move.first]
                if (targetPiece != null) {
                    // MVV-LVA: Most Valuable Victim - Least Valuable Attacker
                    (pieceValues[targetPiece.type()] ?: 0) - (pieceValues[movingPiece?.type()] ?: 0) / 10
                } else {
                    0
                }
            }
        }

        fun evaluate(
            gameState: GameManager.GameState,
            preComputedPossibleMoves: List<Pair<Int, Int>>? = null
        ): Int {
            var score = 0
            score += materialValue(gameState)
            score += pieceSquareTables(gameState)
            score += pawnStructure(gameState)
            return score
        }

        private fun pawnStructure(gameState: GameManager.GameState): Int {
            var score = 0
            score += evaluatePawnsOfColor(gameState, Color.WHITE)
            score -= evaluatePawnsOfColor(gameState, Color.BLACK)
            return score
        }

        private fun evaluatePawnsOfColor(gameState: GameManager.GameState, color: Color?): Int {
            var score = 0
            // Collect all pawns as (row, col) pairs for structural analysis
            val allPawns = mutableListOf<Pair<Int, Int>>()
            for (idx in gameState.chessBoard.indices) {
                val piece = gameState.chessBoard[idx]
                if (piece != null && piece.type() == "Pawn" && piece.color == color) {
                    allPawns.add(Pair(idx / 8, idx % 8))
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
                val direction = if (color == Color.WHITE) -1 else 1
                val hasFriendlyPawnAhead = allPawns.any { it.first == row + direction && it.second == col }
                if (!hasFriendlyPawnAhead) {
                    score -= 25
                }
            }

            // Passed pawns
            for (pawn in allPawns) {
                val row = pawn.first
                val col = pawn.second
                val direction = if (color == Color.WHITE) -1 else 1
                val hasEnemyPawnAhead = gameState.chessBoard.indices.any { idx ->
                    val r = idx / 8
                    val c = idx % 8
                    val piece = gameState.chessBoard[idx]
                    piece != null && piece.type() == "Pawn" && piece.color != color &&
                            c == col && ((direction == -1 && r < row) || (direction == 1 && r > row))
                }
                if (!hasEnemyPawnAhead) {
                    val rank = if (color == Color.WHITE) 7 - row else row
                    score += 20 * rank
                }
            }

            // Chain pawns
            for (pawn in allPawns) {
                val row = pawn.first
                val col = pawn.second
                val direction = if (color == Color.WHITE) -1 else 1
                val hasFriendlyPawnDiagonal = allPawns.any { other ->
                    (other.second == col - 1 || other.second == col + 1) && other.first == row + direction
                }
                if (hasFriendlyPawnDiagonal) {
                    score += 15
                }
            }

            return score
        }

        private fun pieceSquareTables(gameState: GameManager.GameState): Int {
            var value = 0
            for (idx in gameState.chessBoard.indices) {
                val piece = gameState.chessBoard[idx] ?: continue
                val rowIndex = idx / 8
                val colIndex = idx % 8
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
            return value
        }

        private fun materialValue(gameState: GameManager.GameState): Int {
            var value = 0
            for (piece in gameState.chessBoard) {
                if (piece != null) {
                    val pieceValue = pieceValues[piece.type()] ?: 0
                    value += if (piece.color == Color.WHITE) pieceValue else -pieceValue
                }
            }
            return value
        }
    }
}