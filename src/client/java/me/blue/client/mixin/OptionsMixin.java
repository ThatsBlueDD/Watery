package me.blue.client.mixin;

import me.blue.client.gui.ClientGuis;
import me.blue.client.shader.ShaderOptions;

import net.minecraft.client.Options;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
	@Inject(method = "getMenuBackgroundBlurriness", at = @At("HEAD"), cancellable = true)
	private void blue$guiBlur(CallbackInfoReturnable<Integer> cir) {
		int blur = ClientGuis.blur();

		if (blur >= 0) {
			cir.setReturnValue(blur);
		}
	}

	@Inject(method = "processOptions", at = @At("TAIL"))
	private void blue$shaderOptions(@Coerce Object access, CallbackInfo ci) {
		ShaderOptions.processOptions(access);
	}
}
