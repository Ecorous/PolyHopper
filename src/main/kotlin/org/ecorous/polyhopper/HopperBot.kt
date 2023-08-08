package org.ecorous.polyhopper

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.extPluralKit
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.runBlocking
import net.minecraft.text.Text
import org.ecorous.polyhopper.extensions.MainExtension
import org.ecorous.polyhopper.helpers.DiscordMessageSender
import org.ecorous.polyhopper.helpers.ConsoleContext
import org.ecorous.polyhopper.helpers.ChatCommandContext

object HopperBot {

    lateinit var bot: ExtensibleBot

    private lateinit var messageSender: DiscordMessageSender

    suspend fun init() {
        val token = PolyHopper.CONFIG.bot.token

        bot = ExtensibleBot(token) {
            extensions {
                extPluralKit()
                add(::MainExtension)
            }
            presence {
                playing("Minecraft with ${Utils.getPlayerCount()} players!")
            }
        }

        messageSender = PolyHopper.CONFIG.bot.messageMode.constructSender(bot)
    }

    fun onPlayerCountChange() {
        runBlocking {
            bot.kordRef.editPresence {
                playing("Minecraft with ${Utils.getPlayerCount()} players!")
            }
        }
    }

    fun sendMinecraftMessage(context: ChatCommandContext, text: Text) {
        sendMessage(Utils.minecraftTextToDiscordMessage(text), context)
    }

    fun sendEmbed(context: ChatCommandContext = ConsoleContext, body: EmbedBuilder.() -> Unit) {
        messageSender.sendEmbed(context, body)
    }

    fun sendMessage(message: String, context: ChatCommandContext) {
        messageSender.sendMessage(message, context)
    }
}
