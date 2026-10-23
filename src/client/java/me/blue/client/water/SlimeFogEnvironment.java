package me.blue.client.water;

import me.blue.fluid.RealisticSlimeFluid;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class SlimeFogEnvironment extends FogEnvironment {
	private static final Vector3fc COLOR = ARGB.vector3fFromRGB24(0x4E8F3E);

	@Override
	public Vector3fc getBaseColor(final ClientLevel level, final Camera camera, final int renderDistance, final float partialTicks) {
		return COLOR;
	}

	@Override
	public void setupFog(final FogData fog, final Camera camera, final ClientLevel level, final float renderDistance, final DeltaTracker deltaTracker) {
		if (camera.entity().isSpectator()) {
			fog.environmentalStart = -8.0F;
			fog.environmentalEnd = renderDistance * 0.5F;
		} else {
			fog.environmentalStart = 0.0F;
			fog.environmentalEnd = Math.max(WaterSettings.slimeFogDistance, 0.01F);
		}
		fog.skyEnd = fog.environmentalEnd;
		fog.cloudEnd = fog.environmentalEnd;
	}

	@Override
	public boolean isApplicable(final @Nullable FogType fogType, final Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return false;
		}
		Camera camera = mc.gameRenderer.mainCamera();
		return camera.isInitialized() && RealisticSlimeFluid.isRealistic(mc.level.getFluidState(camera.blockPosition()));
	}
}
