package org.ecorous.polyhopper

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.ecorous.polyhopper.helpers.PlayerContext
import org.quiltmc.qkl.library.text.buildText
import java.util.*

object Utils {

    private const val OBFUSCATION_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"

    fun writeLinkedAccounts(linkedAccounts: LinkedAccounts) {
        PolyHopper.linkedAccountsPath.writeText(PolyHopper.gson.toJson(linkedAccounts))
    }

    fun getWebhookUsername(playerContext: PlayerContext): String {
        return PolyHopper.CONFIG.webhook.nameFormat
            .replace("{displayName}", playerContext.displayName)
            .replace("{username}", playerContext.username)
    }

    fun getMessageModeMessage(username: String, displayName: String, content: String): String {
        return PolyHopper.CONFIG.message.messageFormat
            .replace("{username}", username)
            .replace("{displayName}", displayName)
            .replace("{text}", content)
    }


    fun getInGameMessage(message: String, username: String): Text {
        val ingameFormat = PolyHopper.CONFIG.bot.ingameFormat

        // Convert the Discord message to Minecraft text format
        val messageText = discordMessageToMinecraftText(message)

        // Build the final in-game message using the TextBuilder DSL
        return buildText {
            val usernameIndex = ingameFormat.indexOf("{username}")
            val messageIndex = ingameFormat.indexOf("{message}")

            if (usernameIndex < messageIndex) {
                // If {username} appears before {message}
                text.append(Text.of(ingameFormat.substring(0, usernameIndex)))
                text.append(Text.of(username))
                text.append(Text.of(ingameFormat.substring(usernameIndex + "{username}".length, messageIndex)))
                text.append(messageText)
                text.append(Text.of(ingameFormat.substring(messageIndex + "{message}".length)))
            } else {
                // If {message} appears before {username}
                text.append(Text.of(ingameFormat.substring(0, messageIndex)))
                text.append(messageText)
                text.append(Text.of(ingameFormat.substring(messageIndex + "{message}".length, usernameIndex)))
                text.append(Text.of(username))
                text.append(Text.of(ingameFormat.substring(usernameIndex + "{username}".length)))
            }
        }
    }


    fun getMaxPlayerCount(): Int {
        return PolyHopper.server!!.maxPlayerCount
    }

    fun getCurrentPlayerCount(): Int {
        return PolyHopper.server!!.currentPlayerCount
    }

    fun getPlayerCount(): String {
        return "${PolyHopper.server!!.playerManager.currentPlayerCount}/${PolyHopper.server!!.playerManager.maxPlayerCount}"
    }

    fun discordMessageToMinecraftText(message: String): Text {
        //TODO()
        var result: Text = Text.of(message)
        runBlocking {
            var messageResult = message
            val userMentionPattern = """(<@!?([0-9]{16,20})>)""".toRegex()
            for (match in userMentionPattern.findAll(message)) {
                var value = match.value
                val id = Snowflake(value.replace("<@", "").replace(">", ""))
                val user = HopperBot.bot.kordRef.getGuildOrThrow(Snowflake(PolyHopper.CONFIG.bot.guildId))
                    .getMember(id)
                val username = "@" + user.displayName

                messageResult = messageResult.replace(match.value, "§6$username§r")
                result = Text.literal(messageResult)
            }
        }
        return result
    }

    fun minecraftTextToDiscordMessage(message: Text): String {
        val builder = StringBuilder()
        message.visit({ style, text ->
            if (style.isUnderlined) builder.append("__")
            if (style.isBold) builder.append("**")
            if (style.isItalic) builder.append("*")
            if (style.isStrikethrough) builder.append("~~")
            if (style.isObfuscated) {
                builder.append(obfuscatedMessage(text.length)) // todo: better way to obfuscate text?
            } else {
                builder.append(text)
            }
            if (style.isStrikethrough) builder.append("~~")
            if (style.isItalic) builder.append("*")
            if (style.isBold) builder.append("**")
            if (style.isUnderlined) builder.append("__")

            return@visit Optional.empty<String>()
        }, Style.EMPTY)

        return builder.toString()
    }

    fun obfuscatedMessage(length: Int): String {
        val random = Random()
        var rv = ""
        for (i in 0..length) {
            rv += OBFUSCATION_CHARACTERS[random.nextInt(OBFUSCATION_CHARACTERS.length)]
        }
        return rv
    }
}
