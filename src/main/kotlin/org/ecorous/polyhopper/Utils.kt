package org.ecorous.polyhopper

import dev.kord.common.entity.Snowflake
import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TextParserV1
import kotlinx.coroutines.runBlocking
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.ecorous.polyhopper.helpers.ChatCommandContext
import org.quiltmc.qkl.library.text.buildText
import java.util.*

object Utils {

    private const val OBFUSCATION_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
    val PARSER: NodeParser = NodeParser.merge(TextParserV1.SAFE, MarkdownLiteParserV1.ALL)
    val PARSER_CONTEXT: ParserContext = ParserContext.of()

    fun writeLinkedAccounts(linkedAccounts: LinkedAccounts) {
        PolyHopper.linkedAccountsPath.writeText(PolyHopper.gson.toJson(linkedAccounts))
    }

    fun getWebhookUsername(context: ChatCommandContext): String {
        return PolyHopper.CONFIG.webhook.nameFormat
            .replace("{displayName}", context.displayName)
            .replace("{username}", context.username)
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

    fun getPlayerCount(): String {
        return "${PolyHopper.server!!.playerManager.currentPlayerCount}/${PolyHopper.server!!.playerManager.maxPlayerCount}"
    }


    fun discordMessageToMinecraftText(message: String): Text {
        //TODO()
        var result: Text
        runBlocking {
            var messageResult = message
            val userMentionPattern = """(<@!?([0-9]{16,20})>)""".toRegex()
            val channelMentionPattern = """(<#([0-9]{16,20})>)""".toRegex()
            val emojiMentionPattern = """(<a?:([a-zA-Z]{2,32}):[0-9]{16,20}>)""".toRegex()
            for (match in userMentionPattern.findAll(message)) {
                val value = match.value
                val id = Snowflake(value.replace("<@", "").replace(">", ""))
                val user = HopperBot.bot.kordRef.getGuild(Snowflake(PolyHopper.CONFIG.bot.guildId))
                    .getMember(id)
                val username = "@" + user.effectiveName

                messageResult = messageResult.replace(value, "<gold><hover:show_text:'$id'>$username</hover></gold>")
            }
            for (match in channelMentionPattern.findAll(message)) {
                val value = match.value
                val id = Snowflake(value.replace("<#", "").replace(">", ""))
                var channelName: String
                var hoverText: String = id.toString()
                val channel = HopperBot.bot.kordRef.getChannel(id)
                if (channel == null) {
                    channelName = "unknown"
                } else {
                    channelName = channel.data.name.value!!
                }
                messageResult =
                    messageResult.replace(value, "t<gold><hover:show_text:'$hoverText'>#$channelName</hover></gold>".trimIndent().trim())
            }
            for (match in emojiMentionPattern.findAll(message)) {
                val name = match.value.substringAfter(":").substringBefore(":")
                val id = match.value.substringAfterLast(":").replace(">", "")
                messageResult = messageResult.replace(match.value, "<gold><hover:show_text:'$id'>:$name:</hover></gold>")
            }
            result = PARSER.parseText(messageResult, PARSER_CONTEXT)
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
