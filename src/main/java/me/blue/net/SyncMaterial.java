package me.blue.net;

import me.blue.ModContent;
import net.minecraft.world.level.block.Block;

public final class SyncMaterial {
	public static final byte WATER = 0;
	public static final byte SAND = 1;
	public static final byte LAVA = 2;
	public static final byte SLIME = 3;

	private SyncMaterial() {
	}

	public static Block block(byte material) {
		return switch (material) {
			case SAND -> ModContent.REALISTIC_SAND;
			case LAVA -> ModContent.REALISTIC_LAVA_BLOCK;
			case SLIME -> ModContent.REALISTIC_SLIME_BLOCK;
			default -> ModContent.REALISTIC_WATER_BLOCK;
		};
	}

	public static String label(byte material) {
		return switch (material) {
			case SAND -> "sand";
			case LAVA -> "lava";
			case SLIME -> "slime";
			default -> "water";
		};
	}
}
