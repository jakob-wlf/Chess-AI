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
        private const val searchDepth = 5

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

        // Todo: Iterative Deepening Search
        // TODO: Openings Book
        private fun alphabeta(gameState: GameManager.GameState, depth: Int, alpha: Int, beta: Int, maximizing: Boolean, allowNullMove: Boolean = true): Int {
            // Alpha and Beta are a window. Any score outside this window means the opponent will avoid it, so we can stop evaluating that branch (prune it)
            var a = alpha // The best score the maximizing player can guarantee at this point or above
            var b = beta // The best score the minimizing player can guarantee at this point or above

            val possibleMoves = gameState.getPossibleMoves()
            val noMovesLeft = possibleMoves.isEmpty()
            val isInCheck = Chessboard.instance?.isInCheck(if (gameState.isWhiteTurn) Color.WHITE else Color.BLACK, gameState.chessBoard, gameState.moveHistory) ?: false
            val hasMajorPieces = gameState.chessBoard.any { it != null && it.type() != "Pawn" && it.type() != "King" }

            if (noMovesLeft) {
                return if (isInCheck) {
                    // We use depth in the score so the engine prefers faster mates.
                    // If it's white's turn and they're in checkmate, black won — return a large negative score.
                    // If it's black's turn and they're in checkmate, white won — return a large positive score.
                    if (gameState.isWhiteTurn) {
                        -(100000 - depth) // White is checkmated, bad for maximizer
                    } else {
                        100000 - depth  // Black is checkmated, good for maximizer
                    }
                } else {
                    0 // Stalemate — draw
                }
            }

            if (depth == 0) {
                return evaluate(gameState, possibleMoves) // TODO: Use Transposition Table here to avoid re-evaluating positions we've already seen
            }

            // Null Move Pruning
            // Before searching through all moves we try and do nothing. If the score afterward is still >= beta then
            // the position is so overwhelmingly good that we can safely prune the branch because the opponent has a better option somewhere in the tree
            // Allow Null Moves as a safeguard against recursion, hasMajorPieces to avoid zugzwang, depth >= 3 because otherwise (depth - R - 1) could be <= 0
            if(allowNullMove && !isInCheck && hasMajorPieces && depth >= 3) {
                val R = 2 // Reduction depth
                val nullBoard = gameState.chessBoard.copyOf()
                val nullState = GameManager.GameState(nullBoard, gameState.moveHistory.toMutableList(), !gameState.isWhiteTurn)

                // We pass in -b + 1 an -b to create a null window
                // We only care whether the score is better than beta or worse. This small window allows much faster pruning
                val nullScore = alphabeta(nullState, depth - R - 1, b - 1, b, maximizing = !maximizing, allowNullMove = false)

                if(nullScore >= b) {
                    // Fail hard beta cutoff
                    // We return nothing greater than beta, just b alone is enough to cause a cutoff
                    return b
                }
            }

            // Alpha Beta Pruning
            if (maximizing) {
                var value = Int.MIN_VALUE
                for (move in orderMoves(possibleMoves, gameState)) {
                    val tempBoard = gameState.chessBoard.copyOf()
                    val tempState = GameManager.GameState(tempBoard, gameState.moveHistory.toMutableList(), gameState.isWhiteTurn)

                    Chessboard.instance?.makeMove(move.first, move.second, tempState, ignoreKingSafety = false, updateUI = false, printErrors = false)
                    value = maxOf(value, alphabeta(tempState, depth - 1, a, b, maximizing = false))

                    // Raise the lower bound for the maximizer.
                    // The maximizer is guaranteed to get at least this score, so we update alpha
                    a = maxOf(a, value)

                    // Beta cutoff --> Minimizer would not allow this move as they have a better option somewhere else in the tree, so we can stop evaluating this branch
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

                    // Lower the upper bound for the minimizer.
                    // The minimizer is guaranteed to get at most this score, so we update beta (Low score is desirable for the minimizer)
                    b = minOf(b, value)

                    // Alpha cutoff --> Maximizer would not allow this move as they have a better option somewhere else in the tree, so we can stop evaluating this branch
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