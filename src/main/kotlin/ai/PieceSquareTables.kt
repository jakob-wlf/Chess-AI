package ai

class PieceSquareTables {
    companion object {
        private val pawnTableWhite = arrayOf(
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0),
            intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
            intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
            intArrayOf( 5,  5, 10, 27, 27, 10,  5,  5),
            intArrayOf( 0,  0,  0, 25, 25,  0,  0,  0),
            intArrayOf( 5, -5,-10,  0,  0,-10, -5, -5),
            intArrayOf( 5, 10, 10,-25,-25, 10, 10,  5),
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0)
        )
        fun getPawnValue(row: Int, col: Int, isWhite: Boolean): Int {
            return if (isWhite) pawnTableWhite[row][col] else pawnTableWhite[7 - row][col]
        }

        private val KnightTableWhite = arrayOf(
            intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
            intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
            intArrayOf(-10,  0,  5, 10, 10,  5,  0,-10),
            intArrayOf(-10,  5,  5, 10, 10,  5,  5,-10),
            intArrayOf(-10,  0, 10, 10, 10, 10,  0,-10),
            intArrayOf(-10, 10, 10, 10, 10, 10, 10,-10),
            intArrayOf(-10,  5,  0,  0,  0,  0,  5,-10),
            intArrayOf(-20,-10,-40,-10,-10,-40,-10,-20)
        )
        fun getKnightValue(row: Int, col: Int, isWhite: Boolean): Int {
            return if (isWhite) KnightTableWhite[row][col] else KnightTableWhite[7 - row][col]
        }

        private val BishopTable = arrayOf(
            intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
            intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
            intArrayOf(-10,  0,  5, 10, 10,  5,  0,-10),
            intArrayOf(-10,  5,  5, 10, 10,  5,  5,-10),
            intArrayOf(-10,  0, 10, 10, 10, 10,  0,-10),
            intArrayOf(-10, 10, 10, 10, 10, 10, 10,-10),
            intArrayOf(-10,  5,  0,  0,  0,  0,  5,-10),
            intArrayOf(-20,-10,-40,-10,-10,-40,-10,-20)
        )
        fun getBishopValue(row: Int, col: Int, isWhite: Boolean): Int {
            return if (isWhite) BishopTable[row][col] else BishopTable[7 - row][col]
        }

        private val RookTableWhite = arrayOf(
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0),
            intArrayOf( 5, 10, 10, 10, 10, 10, 10,  5),
            intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
            intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
            intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
            intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
            intArrayOf(-5,  0,  0,  0,  0,  0,  0, -5),
            intArrayOf( 0,  0,  0,  5,  5,  0,  0,  0)
        )
        fun getRookValue(row: Int, col: Int, isWhite: Boolean): Int {
            return if (isWhite) RookTableWhite[row][col] else RookTableWhite[7 - row][col]
        }

        private val QueenTableWhite = arrayOf(
            intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20),
            intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
            intArrayOf(-10,  0,  5,  5,  5,  5,  0,-10),
            intArrayOf( -5,  0,  5,  5,  5,  5,  0, -5),
            intArrayOf(  0,  0,  5,  5,  5,  5,  0, -5),
            intArrayOf(-10,  5,  5,  5,  5,  5,  0,-10),
            intArrayOf(-10,  0,  5,  0,  0,  0,  0,-10),
            intArrayOf(-20,-10,-10, -5, -5,-10,-10,-20)
        )
        fun getQueenValue(row: Int, col: Int, isWhite: Boolean): Int {
            return if (isWhite) QueenTableWhite[row][col] else QueenTableWhite[7 - row][col]
        }
    }
}