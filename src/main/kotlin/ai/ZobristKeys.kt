package ai

import GameManager
import java.awt.Color

// Zobrist Hashing
// Every (square, piece-type, color) combination gets a unique random 64-bit key
// at startup. The hash for a position is the XOR of all active keys.
// XOR is self-inverse: toggling the same key twice restores the original value.
object ZobristKeys {

    // White slots 0-5, Black slots 6-11
    // Pawn=0, Knight=1, Bishop=2, Rook=3, Queen=4, King=5
    private val typeIndex = mapOf(
        "Pawn"   to 0, "Knight" to 1, "Bishop" to 2,
        "Rook"   to 3, "Queen"  to 4, "King"   to 5
    )

    val pieceKeys:     Array<LongArray>   // [square 0-63][piece slot 0-11]
    val sideToMove:    Long               // XOR'd in when it is Black's turn
    val castlingKeys:  LongArray          // 16 entries, one per 4-bit castling mask
    val enPassantKeys: LongArray          // indices 0-7 = file, index 8 = "none"

    init {
        val rng = java.util.Random(0x3141592653589793L)  // fixed seed = reproducible
        pieceKeys     = Array(64) { LongArray(12) { rng.nextLong() } }
        sideToMove    = rng.nextLong()
        castlingKeys  = LongArray(16) { rng.nextLong() }
        enPassantKeys = LongArray(9)  { rng.nextLong() }
    }

    fun pieceSlot(type: String, isWhite: Boolean): Int {
        val base = typeIndex[type] ?: error("Unknown piece type: $type")
        return if (isWhite) base else base + 6
    }

    // Computes the Zobrist hash for [gameState] from scratch.
    // Iterates all 64 squares once, then folds in side-to-move,
    // castling rights, and the en-passant file.
    fun computeHash(gameState: GameManager.GameState): Long {
        var hash = 0L

        for (idx in gameState.chessBoard.indices) {
            val piece = gameState.chessBoard[idx] ?: continue
            hash = hash xor pieceKeys[idx][pieceSlot(piece.type(), piece.color == Color.WHITE)]
        }

        if (!gameState.isWhiteTurn) hash = hash xor sideToMove

        var castlingMask = 0
        if (gameState.whiteKingSideCastlePossible)  castlingMask = castlingMask or 1
        if (gameState.whiteQueenSideCastlePossible) castlingMask = castlingMask or 2
        if (gameState.blackKingSideCastlePossible)  castlingMask = castlingMask or 4
        if (gameState.blackQueenSideCastlePossible) castlingMask = castlingMask or 8
        hash = hash xor castlingKeys[castlingMask]

        val epIndex = if (gameState.enPassantCaptureFile < 0) 8 else gameState.enPassantCaptureFile
        hash = hash xor enPassantKeys[epIndex]

        return hash
    }
}

// Transposition Table
/**
 * Fixed-size cache mapping Zobrist hashes to alpha-beta search results.
 *
 * ### Flag meanings
 * | Flag          | Meaning                                           |
 * |---------------|---------------------------------------------------|
 * | EXACT         | score is the true minimax value                   |
 * | LOWER_BOUND   | score is a lower bound (beta cutoff occurred)     |
 * | UPPER_BOUND   | score is an upper bound (alpha was never raised)  |
 *
 * ### Replacement strategy
 * Depth-preferred: a new entry replaces the stored one when
 * (a) the slot is empty, (b) the new search is at least as deep, or
 * (c) the new entry is EXACT.
 */
class TranspositionTable(sizeMb: Int = 64) {

    enum class Flag { EXACT, LOWER_BOUND, UPPER_BOUND }

    data class Entry(
        val hash:  Long,   // full hash retained for collision detection
        val depth: Int,
        val score: Int,
        val flag:  Flag
    )

    // ~40 bytes per Entry on a 64-bit JVM
    private val capacity: Int = ((sizeMb.toLong() * 1024L * 1024L) / 40L)
        .coerceIn(1_024L, Int.MAX_VALUE.toLong()).toInt()

    private val table = arrayOfNulls<Entry>(capacity)

    private fun index(hash: Long): Int = ((hash ushr 1) % capacity).toInt()

    /** Returns the cached entry, or null on a miss or slot collision. */
    fun lookup(hash: Long): Entry? {
        val e = table[index(hash)] ?: return null
        return if (e.hash == hash) e else null
    }

    /** Stores a result using depth-preferred replacement. */
    fun store(hash: Long, depth: Int, score: Int, flag: Flag) {
        val idx      = index(hash)
        val existing = table[idx]
        if (existing == null || depth >= existing.depth || flag == Flag.EXACT) {
            table[idx] = Entry(hash, depth, score, flag)
        }
    }

    fun clear() = table.fill(null)
}