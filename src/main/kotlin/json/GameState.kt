package json

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val moves: String,
)