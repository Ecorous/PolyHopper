package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity

object VanillaContextFactory : PlayerContextFactory {
    override fun getContext(player: ServerPlayerEntity): PlayerContext {
        return PlayerContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, null)
    }
}
