package org.ecorous.polyhopper.helpers

import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TextParserV1
import net.minecraft.server.network.ServerPlayerEntity

abstract class ChatCommandContextFactory {
    abstract fun getContext(player: ServerPlayerEntity): ChatCommandContext

    private val textParser: NodeParser = NodeParser.merge(TextParserV1.SAFE, MarkdownLiteParserV1.ALL)
    private val parserContext: ParserContext = ParserContext.of()

    protected fun getDisplayName(player: ServerPlayerEntity): String {
        return textParser.parseText(player.displayName.string, parserContext).string
    }
}
