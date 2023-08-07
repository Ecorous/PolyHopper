package org.ecorous.polyhopper.config

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.entity.Snowflake
import org.ecorous.polyhopper.helpers.DiscordMessageSender
import org.ecorous.polyhopper.PolyHopper

enum class MessageMode {
    MESSAGE,
    WEBHOOK;

    fun constructSender(bot: ExtensibleBot): DiscordMessageSender {
        val channelId = Snowflake(PolyHopper.CONFIG.bot.channelId)
        val threadId = PolyHopper.CONFIG.bot.threadId.let {
            if (it.isNotEmpty()) Snowflake(it) else null
        }
        return when (this) {
            MESSAGE -> DiscordMessageSender.MessageSender(bot, channelId, threadId)
            WEBHOOK -> DiscordMessageSender.WebhookSender(bot, channelId, threadId)
        }
    }
}
