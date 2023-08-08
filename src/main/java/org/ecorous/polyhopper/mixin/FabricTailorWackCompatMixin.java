package org.ecorous.polyhopper.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.ecorous.polyhopper.compat.fabrictailor.SkinHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public class FabricTailorWackCompatMixin {
	@Shadow
	@Nullable GameProfile profile;

	@Inject(
		method = "acceptPlayer()V",
		at = @At(
			value = "INVOKE",
		target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"
		)
	)
	private void polyhopper$capturePlayerSkin(CallbackInfo ci) {
		var vanillaSkin = profile.getProperties().get("textures").stream().findFirst().map(Property::getValue).orElse(null);

		SkinHolder.INSTANCE.setDefaultSkin(profile.getId().toString(), vanillaSkin);
	}
}
