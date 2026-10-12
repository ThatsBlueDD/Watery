package me.skaffy.client.water;

import me.skaffy.net.SyncMaterial;

public enum Material {
	WATER,
	SAND,
	LAVA;

	float shaderId() {
		return switch (this) {
			case SAND -> 1.0F;
			case LAVA -> 2.0F;
			default -> 0.0F;
		};
	}

	boolean isGranular() {
		return this == SAND;
	}

	boolean isFluidSim() {
		return this == WATER || this == LAVA;
	}

	boolean usesWhitewater() {
		return this == WATER;
	}

	byte syncId() {
		return switch (this) {
			case SAND -> SyncMaterial.SAND;
			case LAVA -> SyncMaterial.LAVA;
			default -> SyncMaterial.WATER;
		};
	}
}
