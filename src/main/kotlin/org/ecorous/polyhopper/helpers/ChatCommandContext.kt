package org.ecorous.polyhopper.helpers

data class ChatCommandContext(
    val uuid: String,
    val username: String,
    val displayName: String,
    val skinId: String?
)

val ConsoleContext = ChatCommandContext("", "Server", "Server", null)
val CommandOutputContext = ChatCommandContext("", "Command Output", "Command Output", null)

fun ChatCommandContext.isPlayer() : Boolean {
    return this != ConsoleContext && this != CommandOutputContext
}
