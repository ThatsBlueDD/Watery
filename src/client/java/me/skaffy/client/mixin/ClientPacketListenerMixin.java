package me.skaffy.client.mixin;

import me.skaffy.client.water.WaterWorld;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleExplosion", at = @At("TAIL"))
	private void realisticWater$onExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
		WaterWorld.get().onExplosion(packet.center(), packet.radius());
	}
}
