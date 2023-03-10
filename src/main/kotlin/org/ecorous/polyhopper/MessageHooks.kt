package org.ecorous.polyhopper

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.ecorous.polyhopper.HopperBot.sendEmbed
import org.ecorous.polyhopper.HopperBot.sendMinecraftMessage

object MessageHooks {

    fun onPlayerDeath(player: ServerPlayerEntity, message: Text) {
        if (PolyHopper.CONFIG.bot.announceDeaths) {
            sendEmbed {
                title = message.string
                color = DISCORD_RED
            }
        }
    }

    fun onAdvancement(player: ServerPlayerEntity, message: Text, advancementDescription: Text) {
        if (PolyHopper.CONFIG.bot.announceAdvancements) {
            sendEmbed {
                title = message.string
                color = DISCORD_GREEN
                description = Utils.minecraftTextToDiscordMessage(advancementDescription)
            }
        }
    }

    fun onPlayerConnected(player: ServerPlayerEntity) {
        if (PolyHopper.CONFIG.bot.announcePlayerJoinLeave) {
            sendEmbed {
                title = player.displayName.string + " has joined the game"
                color = DISCORD_GREEN
            }
        }
    }

    fun onPlayerDisconnected(player: ServerPlayerEntity) {
        if (PolyHopper.CONFIG.bot.announcePlayerJoinLeave) {
            sendEmbed {
                title = player.displayName.string + " has left the game"
                color = DISCORD_RED
            }
        }
    }

    fun onChatMessageSent(player: ServerPlayerEntity, message: Text) {
        sendMinecraftMessage(player.displayName.string, player.uuidAsString, player.displayName.string, message)
        // Example: Player56 said: "Hello World!"
        PolyHopper.LOGGER.info(player.displayName.string + " said: \"${message}\"")
    }

    fun onMeCommand(player: ServerPlayerEntity?, message: String) {
        sendEmbed {
            title = "* ${(player?.displayName?.string ?: "Server")} *${message}*"
            color = DISCORD_BLURPLE
        }
    }

    fun onSayCommand(player: ServerPlayerEntity?, message: String) {
        sendEmbed {
            title =  "[${(player?.displayName?.string ?: "Server")}] ${message}"
            color = DISCORD_BLURPLE
        }
    }

    fun onTellRaw(player: ServerPlayerEntity?, message: Text) {
        sendEmbed {
            title = Utils.minecraftTextToDiscordMessage(message) // we don't want to say who did it!
            color = DISCORD_BLURPLE
        }
    }

    fun onServerStarted() {
        sendEmbed {
            title = "Server started!"
            color = DISCORD_GREEN
        }
    }

    fun onServerShutdown() {
        sendEmbed {
            title = "Server stopped!"
            color = DISCORD_RED
        }
    }
}
