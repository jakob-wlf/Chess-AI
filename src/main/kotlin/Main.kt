
fun main(args: Array<String>) {
    lichess()
}

fun normalGame() {
    val gameManager = GameManager()
    val window = Window()
    val chessboard = Chessboard(window)
}

fun lichess() {
    val lichessIntegration = LichessIntegration()
    LichessIntegration.mainInstance = lichessIntegration

    Thread { LichessIntegration().streamLichessEvents() }.start()
}