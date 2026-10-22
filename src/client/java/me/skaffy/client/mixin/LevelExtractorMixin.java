package me.skaffy.client.mixin;

import me.skaffy.client.shader.ColoredLight;

import net.minecraft.client.renderer.extract.LevelExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
	private void skaffy$lightSection(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
		ColoredLight.sectionChanged(x, y, z);
	}

	@Inject(method = "allChanged", at = @At("HEAD"))
	private void skaffy$lightEverything(CallbackInfo ci) {
		ColoredLight.everythingChanged();
	}
}
