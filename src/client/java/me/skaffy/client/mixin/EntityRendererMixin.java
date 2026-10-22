package me.skaffy.client.mixin;

import me.skaffy.client.shader.ShaderRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "extractShadow", at = @At("TAIL"))
	private void skaffy$noBlobShadow(EntityRenderState state, Minecraft minecraft, Level level, CallbackInfo ci) {
		if (ShaderRenderer.hidesBlobShadows()) {
			state.shadowPieces.clear();
		}
	}
}
