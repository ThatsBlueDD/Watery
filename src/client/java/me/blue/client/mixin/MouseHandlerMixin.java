package me.blue.client.mixin;

import me.blue.client.water.ClipRecorder;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void realisticWater$clipCamera(LocalPlayer player, double dx, double dy) {
		double[] turn = ClipRecorder.get().filterTurn(dx, dy);
		player.turn(turn[0], turn[1]);
	}
}
