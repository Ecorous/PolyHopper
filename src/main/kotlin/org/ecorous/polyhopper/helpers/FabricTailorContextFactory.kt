package org.ecorous.polyhopper.helpers

import net.minecraft.server.network.ServerPlayerEntity
import org.samo_lego.fabrictailor.casts.TailoredPlayer

object FabricTailorContextFactory : ChatCommandContextFactory {
    override fun getContext(player: ServerPlayerEntity): ChatCommandContext {
        if (player is TailoredPlayer) {
            if (player.lastSkinChange != 0L) { // This doesn't work when rejoining
                return ChatCommandContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, player.skinId)
            }
        }

        return return ChatCommandContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, null)
    }
}
