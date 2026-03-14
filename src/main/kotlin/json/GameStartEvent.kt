package json

import kotlinx.serialization.Serializable

@Serializable
data class GameStartEvent(
    val type: String,
    val game: Game
)
