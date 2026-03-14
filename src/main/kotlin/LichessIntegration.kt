import json.BotGameState
import json.GameStartEvent
import json.GameState
import kotlinx.serialization.json.Json
import piece.Pawn
import java.awt.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LichessIntegration {

    val API_TOKEN = "PLACEHOLDER"

    var stopGameStream = false

    var lastUCIMove: String? = null

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        var mainInstance: LichessIntegration? = null
    }

    fun setupNewGame(color: Color, gameId: String) {
        val gameManager = GameManager(gameId, isLichessGame = true, color = color)
        val chessboard = Chessboard(Window())
        lastUCIMove = null
        streamGameState(gameId)
    }


    fun extractMoveFromUCI(uciMove: String): Pair<Pair<Int, Int>?, String?>? {
        if(uciMove.length < 4) return null

        val fromFile = uciMove[0] - 'a'
        val fromRank = uciMove[1] - '1'
        val toFile = uciMove[2] - 'a'
        val toRank = uciMove[3] - '1'

        if(fromFile !in 0..7 || fromRank !in 0..7 || toFile !in 0..7 || toRank !in 0..7) return null

        val fromIndex = (7 - fromRank) * 8 + fromFile
        val toIndex = (7 - toRank) * 8 + toFile

        return Pair(Pair(fromIndex, toIndex), if(uciMove.length > 4) uciMove.substring(4) else null)
    }

    fun streamGameState(gameId: String) {
        stopGameStream = false
        val url = URL("https://lichess.org/api/bot/game/stream/$gameId")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $API_TOKEN")
            setRequestProperty("Accept", "application/x-ndjson")
            connectTimeout = 10_000
            readTimeout = 0 // No timeout — stream stays open
        }

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            println("Error: $responseCode")
            return
        }

        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null && !stopGameStream) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) {
                    continue
                }
                handleGameState(trimmed)
            }
        }
    }

    private fun handleGameState(json: String) {
        println("Game state update: $json")
        val botGameState = if(json.contains("id")) this.json.decodeFromString<BotGameState>(json) else null
        val gameState = botGameState?.state ?: this.json.decodeFromString<GameState>(json)
        val moves = gameState.moves.split(" ")
        val gameManager = GameManager.instance ?: return

        println("Moves so far: ${gameState.moves}")

        if(gameState.moves.isEmpty()) {
            println("First move of the game!")
            if(gameManager.gameState.isWhiteTurn && gameManager.gameState.lichessBotColor == Color.WHITE) {
                val aiMove = ai.ChessAI.getNextMove(gameManager.gameState) ?: return
                sendMove(aiMove, gameManager)
            }
            return
        }
        if(lastUCIMove == null && moves.isNotEmpty()) {
            val moveSublist = moves.subList(0, moves.size - 1)
            for(move in moveSublist) {
                val extractedMove = extractMoveFromUCI(move) ?: continue
                if(extractedMove.first == null) continue
                var pieceToPromoteTo = "queen"
                if(extractedMove.second != null) {
                    pieceToPromoteTo = when(extractedMove.second) {
                        "q" -> "queen"
                        "r" -> "rook"
                        "b" -> "bishop"
                        "n" -> "knight"
                        else -> "queen"
                    }
                }
                gameManager.makeMove(extractedMove.first!!.first, extractedMove.first!!.second, pieceToPromoteTo = pieceToPromoteTo)
            }
        }

        if(moves.last() == lastUCIMove) return
        lastUCIMove = moves.last()
        println(moves)

        println("Processing move: ${moves.last()}")
        val move = extractMoveFromUCI(moves.last()) ?: return
        if(move.first == null) return
        var pieceToPromoteTo = "queen"
        if(move.second != null) {
            pieceToPromoteTo = when(move.second) {
                "q" -> "queen"
                "r" -> "rook"
                "b" -> "bishop"
                "n" -> "knight"
                else -> "queen"
            }
        }
        gameManager.makeMove(move.first!!.first, move.first!!.second, pieceToPromoteTo = pieceToPromoteTo)

        if(gameManager.gameState.isWhiteTurn && gameManager.gameState.lichessBotColor == Color.WHITE ||
           !gameManager.gameState.isWhiteTurn && gameManager.gameState.lichessBotColor == Color.BLACK) {
            println("Bot's turn to move.")
            val aiMove = ai.ChessAI.getNextMove(gameManager.gameState) ?: return
            println("Bot chooses move: ${aiMove.first} to ${aiMove.second}")
            sendMove(aiMove, gameManager)
        }
    }

    private fun sendMove(aiMove: Pair<Int, Int>, gameManager: GameManager) {
        val fromFile = 'a' + aiMove.first % 8
        val fromRank = '1' + (7 - aiMove.first / 8)
        val toFile = 'a' + aiMove.second % 8
        val toRank = '1' + (7 - aiMove.second / 8)

        val isPromotion = gameManager.gameState.chessBoard[aiMove.first] is Pawn && (aiMove.second < 8 || aiMove.second > 55)
        val promotionPiece = if(isPromotion) "q" else ""
        val uciMove = "$fromFile$fromRank$toFile$toRank$promotionPiece"

        val gameId = GameManager.currentGameId ?: return
        val url = URL("https://lichess.org/api/bot/game/$gameId/move/$uciMove")

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $API_TOKEN")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        // Read response
        return BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
    }

    fun streamLichessEvents() {
        val url = URL("https://lichess.org/api/stream/event")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $API_TOKEN")
            setRequestProperty("Accept", "application/x-ndjson")
            connectTimeout = 10_000
            readTimeout = 0 // No timeout — stream stays open
        }

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            println("Error: $responseCode")
            return
        }

        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) {
                    // Keep-alive heartbeat sent every 7 seconds — ignore
                    continue
                }
                handleEvent(trimmed)
            }
        }
    }

    fun handleEvent(json: String) {
        // Parse with your preferred JSON library (e.g. kotlinx.serialization, Gson, Moshi)
        println("Event received: $json")

        when {
            json.contains("\"type\":\"gameStart\"") -> handleGameStart(json)
            json.contains("\"type\":\"gameFinish\"") -> handleGameFinish(json)
            json.contains("\"type\":\"challenge\"") -> println("Challenge received!")
            json.contains("\"type\":\"challengeCanceled\"") -> println("Challenge cancelled!")
            json.contains("\"type\":\"challengeDeclined\"") -> println("Challenge declined!")
        }
    }

    private fun handleGameFinish(json: String) {
        println("Game finished!")
        mainInstance?.stopGameStream = true
    }

    private fun handleGameStart(json: String) {
        println("Game started!")
        mainInstance?.stopGameStream = true
        val event = this.json.decodeFromString<GameStartEvent>(json)
        val color = if (event.game.color == "white") Color.WHITE else Color.BLACK
        println("Bot is playing as ${event.game.color}.")
        mainInstance?.setupNewGame(color, event.game.gameId)
    }
}