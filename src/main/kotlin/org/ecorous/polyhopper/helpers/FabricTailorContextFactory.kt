package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity
import org.samo_lego.fabrictailor.casts.TailoredPlayer

object FabricTailorContextFactory : PlayerContextFactory {
    override fun getContext(player: ServerPlayerEntity): PlayerContext {
        if (player is TailoredPlayer) {
            if (player.skinValue != null) {
                return PlayerContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, player.skinId)
            }
        }

        return return PlayerContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, null)
    }
}
