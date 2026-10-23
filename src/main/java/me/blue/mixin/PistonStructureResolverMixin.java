package me.blue.mixin;

import me.blue.block.RealisticSandBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonStructureResolver.class)
public class PistonStructureResolverMixin {
	@Redirect(method = "addBlockLine", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
	private boolean realisticWater$sandIsAir(BlockState state) {
		return state.isAir() || state.getBlock() instanceof RealisticSandBlock;
	}
}
