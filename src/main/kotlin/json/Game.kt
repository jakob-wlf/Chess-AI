package json

import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val gameId: String,
    val fullId: String,
    val color: String,
)