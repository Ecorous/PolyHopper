package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity

interface ChatCommandContextFactory {
    fun getContext(player: ServerPlayerEntity): ChatCommandContext
}
