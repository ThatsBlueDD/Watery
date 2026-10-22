package me.skaffy.client.mixin;

import java.util.Optional;
import java.util.OptionalDouble;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;

import me.skaffy.client.shader.ShaderRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class ShaderLevelRendererMixin {
	@Unique
	private @Nullable RenderPass skaffy$translucentPass;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Z)V"))
	private void skaffy$shadowMap(LevelRenderer renderer, FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame features, GpuBufferSlice fog,
			ChunkSectionsToRender chunks, boolean consistentDepth, Operation<Void> original) {
		ShaderRenderer.renderShadows(features);
		original.call(renderer, frame, features, fog, chunks, consistentDepth);
	}

	@ModifyVariable(method = "executeClassicTransparency", at = @At("HEAD"), argsOnly = true)
	private RenderPass skaffy$splitSolid(RenderPass solidPass) {
		if (!ShaderRenderer.splitsSolid()) {
			return solidPass;
		}

		solidPass.close();
		ShaderRenderer.afterSolid();
		RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "Translucent", main.getColorTextureView(), Optional.empty(), main.getDepthTextureView(), OptionalDouble.empty());
		RenderSystem.bindDefaultUniforms(pass);
		skaffy$translucentPass = pass;
		return pass;
	}

	@Inject(method = "executeClassicTransparency", at = @At("TAIL"))
	private void skaffy$afterTranslucent(ChunkSectionsToRender chunkSectionsToRender, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo ci) {
		if (skaffy$translucentPass != null) {
			skaffy$translucentPass.close();
			skaffy$translucentPass = null;
		}
	}

	@Inject(method = "executeOit", at = @At("HEAD"))
	private void skaffy$deferredBeforeOit(ChunkSectionsToRender chunkSectionsToRender, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo ci) {
		if (ShaderRenderer.splitsSolid()) {
			ShaderRenderer.afterSolid();
		}
	}
}
