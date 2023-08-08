package org.ecorous.polyhopper.extensions

import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.events.interfaces.MessageEvent
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.types.respond
import com.mojang.authlib.GameProfile
import dev.kord.common.Color
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.WhitelistEntry
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.ecorous.polyhopper.DiscordCommandOutput
import org.ecorous.polyhopper.PolyHopper
import org.ecorous.polyhopper.Utils
import org.ecorous.polyhopper.Utils.getInGameMessage
import java.util.*

@OptIn(KordPreview::class)
class MainExtension : Extension() {
    override val name: String = "main"
    override suspend fun setup() {

        ephemeralSlashCommand(::RunArgs) {
            guild(Snowflake(PolyHopper.CONFIG.bot.guildId))

            name = "run"
            description = "Run a command through the server."

            requirePermission(Permission.Administrator)

            action {
                if (PolyHopper.server == null) {
                    respond {
                        embed {
                            title = "Server might not be started yet! `server` is still null!"
                            color = Color(255, 0, 0)
                        }
                    }
                } else {
                    val source = ServerCommandSource(
                        DiscordCommandOutput(),
                        Vec3d.ZERO,
                        Vec2f.ZERO,
                        PolyHopper.server!!.overworld,
                        4,
                        "Discord (${user.asUser().username})",
                        Text.of("Discord (${user.asUser().username})"),
                        PolyHopper.server!!,
                        null
                    )

                    PolyHopper.server!!.commandManager.executePrefixedCommand(source, arguments.command)
                }
            }
        }

        ephemeralSlashCommand {
            guild(Snowflake(PolyHopper.CONFIG.bot.guildId))

            name = "stop"
            description = "Stops the server."

            requirePermission(Permission.Administrator)

            action {
                PolyHopper.server?.stop(false)

                respond {
                    content = "Server stopped."
                }
            }
        }

        publicSlashCommand {
            guild(Snowflake(PolyHopper.CONFIG.bot.guildId))

            name = "list"
            description = "Lists online players."

            action {
                val players = PolyHopper.server!!.playerManager.playerList.joinToString("\n") { it.displayName.string }

                respond {
                    embed {
                        title = "List of online players (${Utils.getPlayerCount()})"
                        description = players
                    }
                }
            }
        }

        if (PolyHopper.CONFIG.bot.whitelistCommand) {
            ephemeralSlashCommand(::WhitelistArgs) {
                guild(Snowflake(PolyHopper.CONFIG.bot.guildId))

                name = "whitelist"
                description = "Whitelists a user."

                action {
                    val server: MinecraftServer = PolyHopper.server!!

                    server.execute {
                        val playerByUsername: Optional<GameProfile> = server.userCache!!.findByName(arguments.user)
                        val playerManager = server.playerManager
                        var whitelisted = false

                        if (playerByUsername.isPresent) {
                            val gameProfile = playerByUsername.get()

                            if (!playerManager.whitelist.isAllowed(gameProfile)) {
                                playerManager.whitelist.add(WhitelistEntry(gameProfile))
                                whitelisted = true
                            }
                        } else {
                            try {
                                val gameProfile = GameProfile(UUID.fromString(arguments.user), null)

                                if (!playerManager.whitelist.isAllowed(gameProfile)) {
                                    playerManager.whitelist.add(WhitelistEntry(gameProfile))
                                }

                                whitelisted = true
                            } catch (_: IllegalArgumentException) {

                            }
                        }

                        if (whitelisted) {
                            if (PolyHopper.CONFIG.bot.whitelistChannelId.isNotEmpty()) {
                                runBlocking {
                                    val channel = bot.kordRef.getChannelOf<MessageChannel>(Snowflake(PolyHopper.CONFIG.bot.whitelistChannelId))!!
                                    channel.createEmbed {
                                        title = "User whitelisted!"

                                        field {
                                            name = "Minecraft User"
                                            value = arguments.user
                                        }

                                        field {
                                            name = "Discord User"
                                            value = "${user.mention} (${user.id})"
                                        }
                                    }
                                }
                            }

                            runBlocking {
                                respond {
                                    content = "Whitelisted ${arguments.user}."
                                }
                            }
                        } else {
                            runBlocking {
                                respond {
                                    content = "Failed to whitelist."
                                }
                            }
                        }
                    }
                }
            }
        }
        if (PolyHopper.CONFIG.bot.accountLinking) {
            ephemeralSlashCommand(if (PolyHopper.CONFIG.bot.whitelistCommand) ::LinkAccountWhitelistArgs else ::LinkAccountArgs) {

            }
        }

        // todo: Should definitely clean these up and improve implementation like converting discord message to minecraft text.
        event<ProxiedMessageCreateEvent> {
            action {
                if (shouldSendMessageToMinecraft(event)) {
                    val server = PolyHopper.server!!
                    server.execute {
                        // note: display name doesn't include system tag.
                        server.playerManager.broadcastSystemMessage(
                            //Text.literal("PolyHopper - <${event.pkMessage.member?.displayName ?: "???"}> ${event.message.content}"),
                            event.pkMessage.member?.displayName?.let {
                                getInGameMessage(event.message.content, it)
                            },
                            false
                        )
                    }

                }
            }
        }

        event<UnProxiedMessageCreateEvent> {
            action {
                if (shouldSendMessageToMinecraft(event)) {
                    val author: Member? = event.author

                    if (author != null && !author.isBot) {
                        val server = PolyHopper.server!!

                        server.execute {
                            server.playerManager.broadcastSystemMessage(
                                getInGameMessage(event.message.content, author.effectiveName),
                                false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun shouldSendMessageToMinecraft(event: MessageEvent): Boolean {
        val id = PolyHopper.CONFIG.bot.threadId.let { if (it.isNotEmpty()) Snowflake(it) else null } ?: Snowflake(PolyHopper.CONFIG.bot.channelId)

        return event.message?.channelId == id
    }

    inner class RunArgs : Arguments() {
        val command by string {
            name = "command"
            description = "Command to run."
        }
    }

    inner class WhitelistArgs : Arguments() {
        val user by string {
            name = "user"
            description = "User to whitelist."
        }
    }

    inner class LinkAccountArgs : Arguments() {
        val mcUser by string {
            name = "mcUser"
            description = "Minecraft username to link with your discord account."
        }
    }
    inner class LinkAccountWhitelistArgs : Arguments() {
        val mcUser by string {
            name = "mcUser"
            description = "Minecraft username to link with your discord account."
        }
        val whitelist by boolean {
            name = "whitelist"
            description = "Whether to whitelist the user as well as linking"
        }
    }
}
