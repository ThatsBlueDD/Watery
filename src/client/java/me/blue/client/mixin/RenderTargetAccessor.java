package me.blue.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTarget.class)
public interface RenderTargetAccessor {
	@Accessor("colorTexture")
	GpuTexture blue$colorTexture();

	@Accessor("colorTexture")
	void blue$setColorTexture(GpuTexture texture);

	@Accessor("colorTextureView")
	GpuTextureView blue$colorTextureView();

	@Accessor("colorTextureView")
	void blue$setColorTextureView(GpuTextureView view);
}
