package me.blue.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import me.blue.client.shader.ShaderRenderer;
import me.blue.client.water.WaterWorld;
import me.blue.client.shader.lang.Builtins.Stage;

import net.minecraft.client.renderer.GameRenderer;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ShaderGameRendererMixin {
	@Shadow
	private void tryTakeScreenshotIfNeeded() {
		throw new AssertionError();
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void blue$beginShaderFrame(CallbackInfo ci) {
		ShaderRenderer.beginFrame();
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void blue$beginLevel(CallbackInfo ci) {
		ShaderRenderer.beginLevel();
	}

	@ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"))
	private Matrix4f blue$levelProjection(Matrix4f projection) {
		ShaderRenderer.levelProjection(projection);
		WaterWorld.LEVEL_PROJECTION.set(projection);
		return projection;
	}

	@ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private boolean blue$keepWorldDepth(boolean noPostEffects) {
		return noPostEffects && !ShaderRenderer.needsConsistentDepth();
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render3dHud(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/OptionsRenderState;Z)V"))
	private void blue$worldStage(CallbackInfo ci) {
		ShaderRenderer.runStage(Stage.WORLD);
		ShaderRenderer.drawingHand(true);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render3dHud(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/OptionsRenderState;Z)V", shift = At.Shift.AFTER))
	private void blue$handDone(CallbackInfo ci) {
		ShaderRenderer.drawingHand(false);
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;tryTakeScreenshotIfNeeded()V"))
	private void blue$screenshotAfterHdr(GameRenderer renderer, Operation<Void> original) {
		if (!ShaderRenderer.deferScreenshot()) {
			original.call(renderer);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;applyPostEffects()V", shift = At.Shift.AFTER))
	private void blue$screenStage(CallbackInfo ci) {
		ShaderRenderer.runStage(Stage.SCREEN);
		ShaderRenderer.endLevel();

		if (ShaderRenderer.takeScreenshotNow()) {
			tryTakeScreenshotIfNeeded();
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
	private void blue$finalStage(CallbackInfo ci) {
		ShaderRenderer.runStage(Stage.FINAL);
	}
}
