package org.ecorous.polyhopper.helpers

data class PlayerContext(
    val uuid: String,
    val username: String,
    val displayName: String,
    val skinId: String?
)

val ConsoleContext = PlayerContext("", "Server", "Server", null)
val CommandOutputContext = PlayerContext("", "Command Output", "Command Output", null)
