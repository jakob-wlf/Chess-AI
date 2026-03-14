package json

import kotlinx.serialization.Serializable

@Serializable
data class BotGameState(
    val id: String,
    val state: GameState
)
