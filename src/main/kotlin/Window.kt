import piece.Piece
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JLabel

class Window : JFrame(){

    private val windowWidth = 800
    private val windowHeight = 800

    private val boardLabels = Array(8) { Array<JLabel?>(8) { null } }

    private var selectedPiecePosition: Pair<Int, Int>? = null

    init {
        title = "Chess"
        setSize(windowWidth, windowHeight)
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        contentPane.layout = null  // Set layout on contentPane
        isVisible = true
    }

    fun renderBoard(board: Array<Array<Piece?>>) {
        val usableWidth = contentPane.width
        val usableHeight = contentPane.height
        val stepX = usableWidth / 8
        val stepY = usableHeight / 8

        contentPane.removeAll()

        for(i in board.indices) {
            for (j in board[i].indices) {
                val x = j * stepX
                val y = i * stepY

                boardLabels[i][j] = JLabel().apply {
                    setBounds(x, y, stepX, stepY)
                    horizontalAlignment = JLabel.CENTER
                    verticalAlignment = JLabel.CENTER
                    background = if ((i + j) % 2 == 0)
                        Color.getHSBColor(0.1028f, 0.25f, 0.94f)
                    else
                        Color.getHSBColor(0.075f, 0.45f, 0.71f)

                    isOpaque = true
                    text = board[i][j]?.symbol() ?: ""
                    font = font.deriveFont((stepY * 0.6).toFloat())
                    foreground = if(board[i][j]?.color == Color.WHITE) Color.getHSBColor(0.083f, 0.35f, 0.85f) else Color.getHSBColor(0.0f, 0.0f, 0.0f)

                    val gameManager = GameManager.instance?: return
                    if(gameManager.gameState.onlyOnePlayer && gameManager.gameState.isSoloPlayerWhite && !gameManager.gameState.isWhiteTurn) {
                        return@apply
                    }

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent?) {
                            if(selectedPiecePosition == null) {
                                colorPossibleMoves(board, i, j)
                            }
                        }

                        override fun mouseClicked(e: MouseEvent?) {

                            if(selectedPiecePosition == null) {
                                board[i][j]?: return
                                if((board[i][j]!!.color == Color.WHITE && !gameManager.gameState.isWhiteTurn) || (board[i][j]!!.color == Color.BLACK && gameManager.gameState.isWhiteTurn)) {
                                    return
                                }
                                selectedPiecePosition = Pair(i, j)
                                colorPossibleMoves(board, i, j)
                            } else {
                                val from = selectedPiecePosition!!
                                val to = Pair(i, j)
                                val piece = board[from.first][from.second] ?: return
                                if(!piece.isValidMove(from, to, board, gameManager.gameState.moveHistory, false)) {
                                    selectedPiecePosition = Pair(i, j)
                                    colorPossibleMoves(board, i, j)
                                    return
                                }
                                gameManager.makeMove(from, to)
                                selectedPiecePosition = null
                            }
                        }
                    })
                }
                contentPane.add(boardLabels[i][j])
            }
        }

        contentPane.revalidate()
        contentPane.repaint()
    }

    private fun colorPossibleMoves(board: Array<Array<Piece?>>, i: Int, j: Int) {
        val piece = board[i][j]
        if(piece == null) {
            renderBoard(board)
            return
        }
        val gameManager = GameManager.instance?: return
        if(piece.color == Color.WHITE && !gameManager.gameState.isWhiteTurn ||
            piece.color == Color.BLACK && gameManager.gameState.isWhiteTurn) {
            return
        }

        // Reset all squares to original colors first
        for(row in boardLabels.indices) {
            for(col in boardLabels[row].indices) {
                boardLabels[row][col]?.background = if ((row + col) % 2 == 0)
                    Color.getHSBColor(0.1028f, 0.25f, 0.94f)
                else
                    Color.getHSBColor(0.075f, 0.45f, 0.71f)

                // Reset text color to original
                val pieceAtSquare = board[row][col]
                boardLabels[row][col]?.foreground = if(pieceAtSquare?.color == Color.WHITE)
                    Color.getHSBColor(0.083f, 0.35f, 0.85f)
                else
                    Color.getHSBColor(0.0f, 0.0f, 0.0f)
            }
        }

        boardLabels[i][j]?.background = if((i + j) % 2 == 0) Color.getHSBColor(0.111f, 0.30f, 0.90f) else Color.getHSBColor(0.111f, 0.30f, 0.50f)

        // Make the selected piece text stand out with a bright color
        if(selectedPiecePosition != null && selectedPiecePosition == Pair(i, j)) {
            boardLabels[i][j]?.foreground = Color.getHSBColor(0.111f, 0.90f, 1.0f)  // Bright yellow
        }

        // Color possible moves green
        piece.possibleMoves(Pair(i, j), board, gameManager.gameState.moveHistory, false).forEach {
            boardLabels[it.first][it.second]?.background = if((it.first + it.second) % 2 == 0) Color.getHSBColor(0.556f, 0.40f, 0.85f) else Color.getHSBColor(0.556f, 0.40f, 0.55f)
        }

        contentPane.repaint()
    }
}