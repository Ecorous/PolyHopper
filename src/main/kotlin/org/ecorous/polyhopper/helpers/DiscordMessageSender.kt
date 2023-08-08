package org.ecorous.polyhopper.helpers

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.ensureWebhook
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.execute
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.ecorous.polyhopper.PolyHopper
import org.ecorous.polyhopper.Utils
import java.lang.IllegalStateException

sealed class DiscordMessageSender(val bot: ExtensibleBot, val channelId: Snowflake, val threadId: Snowflake?) : CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    abstract fun sendEmbed(playerContext: PlayerContext, body: EmbedBuilder.() -> Unit)

    protected abstract fun sendMessageInternal(message: String, playerContext: PlayerContext)

    fun sendMessage(message: String, playerContext: PlayerContext) {
        if (passesProxyBlacklist(message)) {
            sendMessageInternal(message, playerContext)
        }
    }

    private fun passesProxyBlacklist(message: String) : Boolean {
        return !PolyHopper.CONFIG.bot.minecraftProxyBlacklist.any { it.isNotEmpty() && message.startsWith(it) }
    }

    protected fun getAvatarUrl(playerContext: PlayerContext): String {
        return when(playerContext) {
            ConsoleContext, CommandOutputContext -> PolyHopper.CONFIG.webhook.serverAvatarUrl
            else -> {
                if (playerContext.skinId != null) {
                    PolyHopper.CONFIG.webhook.fabricTailorAvatarUrl.replace("{skin_id}", playerContext.skinId)
                } else {
                    PolyHopper.CONFIG.webhook.playerAvatarUrl.replace("{uuid}", playerContext.uuid).replace("{username}", playerContext.username)
                }
            }
        }
    }

    class MessageSender(bot: ExtensibleBot, channelId: Snowflake, threadId: Snowflake?) : DiscordMessageSender(bot, channelId, threadId) {
        override fun sendEmbed(playerContext: PlayerContext, body: EmbedBuilder.() -> Unit) {
            launch {
                getChannel().createEmbed(body)
            }
        }

        override fun sendMessageInternal(message: String, playerContext: PlayerContext) {
            launch {
                getChannel().createMessage(
                    PolyHopper.CONFIG.message.messageFormat
                        .replace("{username}", playerContext.username)
                        .replace("{displayName}", playerContext.displayName)
                        .replace("{text}", message)
                )
            }
        }

        private suspend fun getChannel() : MessageChannel {
            return bot.kordRef.getChannelOf<MessageChannel>(threadId ?: channelId)
                ?: throw IllegalStateException("Failed to find channel with id: $channelId, please correct the PolyHopper config.")
        }
    }

    class WebhookSender(bot: ExtensibleBot, channelId: Snowflake, threadId: Snowflake?) : DiscordMessageSender(bot, channelId, threadId) {
        override fun sendEmbed(playerContext: PlayerContext, body: EmbedBuilder.() -> Unit) {
            launch {
                usingWebhook {
                    avatarUrl = getAvatarUrl(ConsoleContext)
                    if (playerContext != ConsoleContext) username = Utils.getWebhookUsername(playerContext)
                    embed(body)
                }
            }
        }

        override fun sendMessageInternal(message: String, playerContext: PlayerContext) {
            launch {
                usingWebhook {
                    avatarUrl = getAvatarUrl(playerContext)
                    username = Utils.getWebhookUsername(playerContext)
                    content = message
                }
            }
        }

        private suspend fun usingWebhook(block: WebhookMessageCreateBuilder.() -> Unit) {
            val webhook = bot.kordRef.getChannelOf<TextChannel>(channelId)
                ?.let { ensureWebhook(it, Utils.getWebhookUsername(ConsoleContext)) }
                ?: throw IllegalStateException("Failed to find channel with id: $channelId, please correct the PolyHopper config.")

            webhook.execute(webhook.token!!, threadId, builder = block)
        }
    }
}
