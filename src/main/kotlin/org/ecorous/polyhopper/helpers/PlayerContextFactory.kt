package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity

sealed interface PlayerContextFactory {
    fun getContext(player: ServerPlayerEntity): PlayerContext
}
