package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity

object VanillaContextFactory : ChatCommandContextFactory {
    override fun getContext(player: ServerPlayerEntity): ChatCommandContext {
        return ChatCommandContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, null)
    }
}
