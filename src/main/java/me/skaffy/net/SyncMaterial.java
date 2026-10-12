package me.skaffy.net;

import me.skaffy.ModContent;
import net.minecraft.world.level.block.Block;

public final class SyncMaterial {
	public static final byte WATER = 0;
	public static final byte SAND = 1;
	public static final byte LAVA = 2;

	private SyncMaterial() {
	}

	public static Block block(byte material) {
		return switch (material) {
			case SAND -> ModContent.REALISTIC_SAND;
			case LAVA -> ModContent.REALISTIC_LAVA_BLOCK;
			default -> ModContent.REALISTIC_WATER_BLOCK;
		};
	}

	public static String label(byte material) {
		return switch (material) {
			case SAND -> "sand";
			case LAVA -> "lava";
			default -> "water";
		};
	}
}
