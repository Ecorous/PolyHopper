package org.ecorous.polyhopper

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.extPluralKit
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.text.Text
import org.ecorous.polyhopper.extensions.MainExtension
import org.ecorous.polyhopper.helpers.DiscordMessageSender
import org.ecorous.polyhopper.helpers.ConsoleContext
import org.ecorous.polyhopper.helpers.ChatCommandContext

object HopperBot {
    lateinit var bot: ExtensibleBot

    private lateinit var messageSender: DiscordMessageSender

    suspend fun init(server: MinecraftServer) {
        val token = PolyHopper.CONFIG.bot.token

        val maxPlayerCount = when (server) {
            is DedicatedServer -> server.properties.maxPlayers
            is IntegratedServer -> 8
            else -> 1
        }

        bot = ExtensibleBot(token) {
            extensions {
                extPluralKit()
                add(::MainExtension)
            }
            presence { // Since we start the bot earlier now, we don't have access to player list yet.
                playing("Minecraft with 0/${maxPlayerCount} players!")
            }
        }.also {
            messageSender = PolyHopper.CONFIG.bot.messageMode.constructSender(it)
        }
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
