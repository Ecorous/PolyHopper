package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity

sealed interface ChatCommandContextFactory {
    fun getContext(player: ServerPlayerEntity): ChatCommandContext
}
