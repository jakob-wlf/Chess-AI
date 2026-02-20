import piece.Piece
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JLabel

class Window : JFrame() {

    private val windowWidth = 800
    private val windowHeight = 800

    private val boardLabels = Array<JLabel?>(64) { null }

    private var selectedPiecePosition: Int? = null

    init {
        title = "Chess"
        setSize(windowWidth, windowHeight)
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        contentPane.layout = null
        isVisible = true
    }

    fun renderBoard(board: Array<Piece?>) {
        val usableWidth = contentPane.width
        val usableHeight = contentPane.height
        val stepX = usableWidth / 8
        val stepY = usableHeight / 8

        contentPane.removeAll()

        for (i in 0..7) {
            for (j in 0..7) {
                val pos = i * 8 + j
                val x = j * stepX
                val y = i * stepY

                boardLabels[pos] = JLabel().apply {
                    setBounds(x, y, stepX, stepY)
                    horizontalAlignment = JLabel.CENTER
                    verticalAlignment = JLabel.CENTER
                    background = if ((i + j) % 2 == 0)
                        Color.getHSBColor(0.1028f, 0.25f, 0.94f)
                    else
                        Color.getHSBColor(0.075f, 0.45f, 0.71f)

                    isOpaque = true
                    text = board[pos]?.symbol() ?: ""
                    font = font.deriveFont((stepY * 0.6).toFloat())
                    foreground = if (board[pos]?.color == Color.WHITE)
                        Color.getHSBColor(0.083f, 0.35f, 0.85f)
                    else
                        Color.getHSBColor(0.0f, 0.0f, 0.0f)

                    val gameManager = GameManager.instance ?: return
                    if (gameManager.gameState.onlyOnePlayer && gameManager.gameState.isSoloPlayerWhite && !gameManager.gameState.isWhiteTurn) {
                        return@apply
                    }

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent?) {
                            if (selectedPiecePosition == null) {
                                colorPossibleMoves(board, pos)
                            }
                        }

                        override fun mouseClicked(e: MouseEvent?) {
                            if (selectedPiecePosition == null) {
                                board[pos] ?: return
                                if ((board[pos]!!.color == Color.WHITE && !gameManager.gameState.isWhiteTurn) ||
                                    (board[pos]!!.color == Color.BLACK && gameManager.gameState.isWhiteTurn)
                                ) {
                                    return
                                }
                                selectedPiecePosition = pos
                                colorPossibleMoves(board, pos)
                            } else {
                                val from = selectedPiecePosition!!
                                val piece = board[from] ?: return
                                if (!piece.isValidMove(from, pos, board, gameManager.gameState.moveHistory, false)) {
                                    selectedPiecePosition = pos
                                    colorPossibleMoves(board, pos)
                                    return
                                }
                                gameManager.makeMove(from, pos)
                                selectedPiecePosition = null
                            }
                        }
                    })
                }
                contentPane.add(boardLabels[pos])
            }
        }

        contentPane.revalidate()
        contentPane.repaint()
    }

    private fun colorPossibleMoves(board: Array<Piece?>, pos: Int) {
        val piece = board[pos]
        if (piece == null) {
            renderBoard(board)
            return
        }
        val gameManager = GameManager.instance ?: return
        if (piece.color == Color.WHITE && !gameManager.gameState.isWhiteTurn ||
            piece.color == Color.BLACK && gameManager.gameState.isWhiteTurn
        ) {
            return
        }

        // Reset all squares to original colors first
        for (idx in 0..63) {
            val row = idx / 8
            val col = idx % 8
            boardLabels[idx]?.background = if ((row + col) % 2 == 0)
                Color.getHSBColor(0.1028f, 0.25f, 0.94f)
            else
                Color.getHSBColor(0.075f, 0.45f, 0.71f)

            val pieceAtSquare = board[idx]
            boardLabels[idx]?.foreground = if (pieceAtSquare?.color == Color.WHITE)
                Color.getHSBColor(0.083f, 0.35f, 0.85f)
            else
                Color.getHSBColor(0.0f, 0.0f, 0.0f)
        }

        val row = pos / 8
        val col = pos % 8
        boardLabels[pos]?.background = if ((row + col) % 2 == 0)
            Color.getHSBColor(0.111f, 0.30f, 0.90f)
        else
            Color.getHSBColor(0.111f, 0.30f, 0.50f)

        // Make the selected piece text stand out with a bright color
        if (selectedPiecePosition != null && selectedPiecePosition == pos) {
            boardLabels[pos]?.foreground = Color.getHSBColor(0.111f, 0.90f, 1.0f)
        }

        // Color possible moves green
        piece.possibleMoves(pos, board, gameManager.gameState.moveHistory, false).forEach { movePos ->
            val mRow = movePos / 8
            val mCol = movePos % 8
            boardLabels[movePos]?.background = if ((mRow + mCol) % 2 == 0)
                Color.getHSBColor(0.556f, 0.40f, 0.85f)
            else
                Color.getHSBColor(0.556f, 0.40f, 0.55f)
        }

        contentPane.repaint()
    }
}