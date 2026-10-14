package me.skaffy.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class RealisticLavaBlock extends RealisticWaterBlock {
	public RealisticLavaBlock(FlowingFluid fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	protected boolean propagatesSkylightDown(final BlockState state) {
		return false;
	}
}
