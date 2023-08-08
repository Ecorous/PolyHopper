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

    abstract fun sendEmbed(context: ChatCommandContext, body: EmbedBuilder.() -> Unit)

    protected abstract fun sendMessageInternal(message: String, context: ChatCommandContext)

    fun sendMessage(message: String, context: ChatCommandContext) {
        if (passesProxyBlacklist(message)) {
            sendMessageInternal(message, context)
        }
    }

    private fun passesProxyBlacklist(message: String) : Boolean {
        return !PolyHopper.CONFIG.bot.minecraftProxyBlacklist.any { it.isNotEmpty() && message.startsWith(it) }
    }

    protected fun getAvatarUrl(context: ChatCommandContext): String {
        return if (context.isPlayer()) {
            if (context.skinId != null) {
                PolyHopper.CONFIG.webhook.fabricTailorAvatarUrl.replace("{skin_id}", context.skinId)
            } else {
                PolyHopper.CONFIG.webhook.playerAvatarUrl.replace("{uuid}", context.uuid).replace("{username}", context.username)
            }
        } else {
            PolyHopper.CONFIG.webhook.serverAvatarUrl
        }
    }

    class MessageSender(bot: ExtensibleBot, channelId: Snowflake, threadId: Snowflake?) : DiscordMessageSender(bot, channelId, threadId) {
        override fun sendEmbed(context: ChatCommandContext, body: EmbedBuilder.() -> Unit) {
            launch {
                getChannel().createEmbed(body)
            }
        }

        override fun sendMessageInternal(message: String, context: ChatCommandContext) {
            launch {
                getChannel().createMessage(
                    PolyHopper.CONFIG.message.messageFormat
                        .replace("{username}", context.username)
                        .replace("{displayName}", context.displayName)
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
        override fun sendEmbed(context: ChatCommandContext, body: EmbedBuilder.() -> Unit) {
            launch {
                usingWebhook {
                    avatarUrl = getAvatarUrl(ConsoleContext)
                    if (context.isPlayer()) username = Utils.getWebhookUsername(context)
                    embed(body)
                }
            }
        }

        override fun sendMessageInternal(message: String, context: ChatCommandContext) {
            launch {
                usingWebhook {
                    avatarUrl = getAvatarUrl(context)
                    username = Utils.getWebhookUsername(context)
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
