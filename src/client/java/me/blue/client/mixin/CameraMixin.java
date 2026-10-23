package me.blue.client.mixin;

import me.blue.fluid.RealisticLavaFluid;
import me.blue.fluid.RealisticSlimeFluid;
import me.blue.fluid.RealisticWaterFluid;
import net.minecraft.client.Camera;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
	@Shadow
	private boolean initialized;
	@Shadow
	private Level level;

	@Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
	private void realisticWater$noVanillaFog(CallbackInfoReturnable<FogType> cir) {
		if (!this.initialized || this.level == null) {
			return;
		}
		FluidState fluid = this.level.getFluidState(((Camera) (Object) this).blockPosition());
		if (RealisticWaterFluid.isRealistic(fluid) || RealisticLavaFluid.isRealistic(fluid) || RealisticSlimeFluid.isRealistic(fluid)) {
			cir.setReturnValue(FogType.NONE);
		}
	}
}
