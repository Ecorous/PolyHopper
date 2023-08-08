package org.ecorous.polyhopper.compat.fabrictailor

import net.minecraft.server.network.ServerPlayerEntity
import org.ecorous.polyhopper.helpers.ChatCommandContext
import org.ecorous.polyhopper.helpers.ChatCommandContextFactory
import org.samo_lego.fabrictailor.casts.TailoredPlayer

object FabricTailorContextFactory : ChatCommandContextFactory {
    override fun getContext(player: ServerPlayerEntity): ChatCommandContext {
        val skinId : String? = player.let {
            if (it is TailoredPlayer) {
                val accountSkin = SkinHolder.getDefaultSkin(it.uuidAsString)
                val currentSkin = it.skinValue

                if (currentSkin != null && currentSkin != accountSkin) {
                    return@let it.skinId
                }
            }

            return@let null
        }

        return return ChatCommandContext(player.uuidAsString, player.gameProfile.name, player.displayName.string, skinId)
    }
}
