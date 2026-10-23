package me.blue.client.water;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import me.blue.client.shader.ShaderRenderer;
import me.blue.client.vk.ComputeProgram;
import me.blue.client.vk.Desc;
import me.blue.client.vk.GraphicsProgram;
import me.blue.client.vk.Program;
import me.blue.client.vk.Sampler;
import me.blue.client.vk.Vk;
import me.blue.client.vk.VkBuf;
import me.blue.client.vk.VkTex;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkFormatProperties;

public final class WaterRenderer implements AutoCloseable {
	private static final int RGBA32F = VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
	private static final Identifier SLIME_TEXTURE = Identifier.withDefaultNamespace("textures/block/slime_block.png");

	private final int thicknessFormat;
	private final GraphicsProgram meshFront;
	private final GraphicsProgram dropletFront;
	private final GraphicsProgram meshThickness;
	private final GraphicsProgram dropletThickness;
	private final GraphicsProgram whitewaterProgram;
	private final Map<Integer, GraphicsProgram> composites = new HashMap<>();
	private final ComputeProgram dropletCull;
	private final Sampler nearest = new Sampler(false);
	private final Sampler linear = new Sampler(true);
	private final VkBuf slimeTexture = SandRenderer.loadTexture(SLIME_TEXTURE, 0xFF7EBF6E);

	private int width;
	private int height;
	private int colorFormat;
	private VkTex sceneDepth;
	private VkTex fluidZ;
	private VkTex sceneColor;
	private VkTex gbuffer;
	private VkTex materialId;
	private VkTex thickness;
	private VkTex foam;

	public WaterRenderer() {
		this.thicknessFormat = blendable(VkTex.R32F) ? VkTex.R32F : VkTex.R16F;
		int U = Desc.UBO;
		int S = Desc.SSBO;
		int T = Desc.TEX;
		GraphicsProgram.State front = new GraphicsProgram.State(RGBA32F, VkTex.D32, true, true, VK10.VK_COMPARE_OP_GREATER, GraphicsProgram.Blend.NONE,
			VkTex.RGBA16F);
		GraphicsProgram.State add = new GraphicsProgram.State(this.thicknessFormat, VK10.VK_FORMAT_UNDEFINED, false, false, VK10.VK_COMPARE_OP_ALWAYS,
			GraphicsProgram.Blend.ADD);
		Map<String, String> droplets = Map.of("DROPLETS", "1", "VIEW_BINDING", "3");
		this.meshFront = new GraphicsProgram("mesh.vert", "mesh_front.frag", Map.of(), front, U, S, S, S, S);
		this.dropletFront = new GraphicsProgram("particle.vert", "droplet_front.frag", droplets, front, U, S, S, S, S);
		this.meshThickness = new GraphicsProgram("mesh.vert", "mesh_thickness.frag", Map.of(), add, U, S, S, S, T);
		this.dropletThickness = new GraphicsProgram("particle.vert", "droplet_thickness.frag", droplets, add, U, S, S, S, T);
		this.whitewaterProgram = new GraphicsProgram("particle.vert", "whitewater.frag", Map.of("VIEW_BINDING", "4"),
			new GraphicsProgram.State(VkTex.RGBA16F, VK10.VK_FORMAT_UNDEFINED, false, false, VK10.VK_COMPARE_OP_ALWAYS, GraphicsProgram.Blend.ADD),
			U, S, T, T, S);
		this.dropletCull = new ComputeProgram("droplet_cull.comp", U, S, S, S, S, S, S);
	}

	private GraphicsProgram composite(int colorFormat) {
		return this.composites.computeIfAbsent(colorFormat, format -> new GraphicsProgram("fullscreen.vert", "composite.frag", Map.of(),
			new GraphicsProgram.State(format, VkTex.D32, true, true, VK10.VK_COMPARE_OP_ALWAYS, GraphicsProgram.Blend.NONE),
			Desc.UBO, Desc.TEX, Desc.TEX, Desc.TEX, Desc.TEX, Desc.TEX, Desc.TEX, Desc.SSBO, Desc.TEX, Desc.SSBO, Desc.TEX, Desc.TEX, Desc.TEX,
			Desc.TEX));
	}

	private static boolean blendable(int format) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			VkFormatProperties props = VkFormatProperties.calloc(stack);
			VK10.vkGetPhysicalDeviceFormatProperties(Vk.vk().getPhysicalDevice(), format, props);
			return (props.optimalTilingFeatures() & VK10.VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT) != 0;
		}
	}

	private void ensureTargets(int w, int h, int colorFormat) {
		if (w == this.width && h == this.height && colorFormat == this.colorFormat && this.sceneDepth != null) {
			return;
		}
		this.releaseTargets();
		this.width = w;
		this.height = h;
		this.colorFormat = colorFormat;
		this.sceneDepth = new VkTex(VkTex.D32, w, h, VkTex.COPY_DST | VkTex.SAMPLED);
		this.fluidZ = new VkTex(VkTex.D32, w, h, VkTex.COPY_DST | VkTex.DEPTH);
		this.sceneColor = new VkTex(colorFormat, w, h, VkTex.COPY_DST | VkTex.SAMPLED);
		this.gbuffer = new VkTex(RGBA32F, w, h, VkTex.COLOR | VkTex.SAMPLED);
		this.materialId = new VkTex(VkTex.RGBA16F, w, h, VkTex.COLOR | VkTex.SAMPLED);
		this.thickness = new VkTex(this.thicknessFormat, w, h, VkTex.COLOR | VkTex.SAMPLED);
		this.foam = new VkTex(VkTex.RGBA16F, w, h, VkTex.COLOR | VkTex.SAMPLED);
	}

	private void releaseTargets() {
		for (VkTex t : new VkTex[]{this.sceneDepth, this.fluidZ, this.sceneColor, this.gbuffer, this.materialId, this.thickness, this.foam}) {
			if (t != null) {
				Vk.destroyLater(t);
			}
		}
		this.sceneDepth = null;
	}

	public void prepare(int w, int h, int colorFormat) {
		this.ensureTargets(w, h, colorFormat);
	}

	public record Body(WaterRegion region, Desc ubo) {
	}

	public record LightViews(long lightmap, long colorA, long colorB, long colorTint) {
	}

	public void record(VkCommandBuffer cb, List<Body> bodies, Desc view, long mainColorImage, long mainColorView, long mainDepthImage,
		long mainDepthView, LightViews lights) {
		int w = this.width;
		int h = this.height;
		for (Body body : bodies) {
			WaterRegion r = body.region();
			if (r.takeFieldStale()) {
				boolean sort = WaterSettings.surfaceGather && WaterSettings.sortParticles;
				r.mesher.build(cb, body.ubo(), r.currentParticles(), r.counters, WaterRegion.C_FLUID + r.cur, r.args, r.staticSolid, r.mesher.fineBox(r.box),
					WaterSettings.sheetFill, sort ? r.spareParticles() : null, WaterRegion.C_FLUID + 1 - r.cur);
				if (sort) {
					r.useSpareParticles();
				}
			}
		}
		for (Body body : bodies) {
			WaterRegion r = body.region();
			this.dropletCull.bind(cb, body.ubo(), Desc.ssbo(r.currentParticles()), Desc.ssbo(r.mesher.densA), view, Desc.ssbo(r.args), Desc.ssbo(r.counters),
				Desc.ssbo(r.dropletIds));
			this.dropletCull.push(cb, WaterRegion.C_FLUID + r.cur);
			this.dropletCull.dispatchIndirect(cb, r.args, WaterRegion.ARGS_FLUID_DISPATCH);
		}

		VkTex.copy(cb, mainDepthImage, this.sceneDepth.image, VK10.VK_IMAGE_ASPECT_DEPTH_BIT, w, h);
		VkTex.copy(cb, mainDepthImage, this.fluidZ.image, VK10.VK_IMAGE_ASPECT_DEPTH_BIT, w, h);
		VkTex.copy(cb, mainColorImage, this.sceneColor.image, VK10.VK_IMAGE_ASPECT_COLOR_BIT, w, h);
		Vk.barrier(cb);
		Desc depthTex = Desc.tex(this.sceneDepth, this.nearest.handle);

		GraphicsProgram.begin(cb, w, h, this.gbuffer.view, new float[]{0, 0, 0, 0}, this.materialId.view, new float[]{-1, 0, 0, 0}, this.fluidZ.view);
		for (Body body : bodies) {
			WaterRegion r = body.region();
			float radius = WaterSettings.particleRadius / r.res;
			this.meshFront.bind(cb, body.ubo(), Desc.ssbo(r.mesher.verts), Desc.ssbo(r.mesher.quads), view, Desc.ssbo(r.light()));
			this.meshFront.drawIndirect(cb, r.mesher.meshCounters, SurfaceMesher.DRAW_ARGS_OFFSET);
			this.dropletFront.bind(cb, body.ubo(), Desc.ssbo(r.currentParticles()), Desc.ssbo(r.dropletIds), view, Desc.ssbo(r.light()));
			this.dropletFront.push(cb, Program.f(radius), Program.f(0.0F), Program.f(0.15F));
			this.dropletFront.drawIndirect(cb, r.args, WaterRegion.ARGS_DROPLET_DRAW);
		}
		GraphicsProgram.end(cb);

		GraphicsProgram.begin(cb, w, h, this.thickness.view, new float[]{0, 0, 0, 0}, 0L);
		for (Body body : bodies) {
			WaterRegion r = body.region();
			float radius = WaterSettings.particleRadius / r.res;
			this.meshThickness.bind(cb, body.ubo(), Desc.ssbo(r.mesher.verts), Desc.ssbo(r.mesher.quads), view, depthTex);
			this.meshThickness.drawIndirect(cb, r.mesher.meshCounters, SurfaceMesher.DRAW_ARGS_OFFSET);
			this.dropletThickness.bind(cb, body.ubo(), Desc.ssbo(r.currentParticles()), Desc.ssbo(r.dropletIds), view, depthTex);
			this.dropletThickness.push(cb, Program.f(radius), Program.f(0.0F), Program.f(0.15F));
			this.dropletThickness.drawIndirect(cb, r.args, WaterRegion.ARGS_DROPLET_DRAW);
		}
		GraphicsProgram.end(cb);
		Vk.barrier(cb);

		GraphicsProgram.begin(cb, w, h, this.foam.view, new float[]{0, 0, 0, 0}, 0L);
		if (WaterSettings.whitewater) {
			for (Body body : bodies) {
				WaterRegion r = body.region();
				if (!r.material.usesWhitewater()) {
					continue;
				}
				this.whitewaterProgram.bind(cb, body.ubo(), Desc.ssbo(r.currentWhitewater()), depthTex, Desc.tex(this.gbuffer, this.nearest.handle), view);
				this.whitewaterProgram.push(cb, Program.f(WaterSettings.wwRadius), Program.f(0.5F), Program.f(0.3F), Program.f(WaterSettings.wwMaxPixels),
					Program.f(WaterSettings.foamSize));
				this.whitewaterProgram.drawIndirect(cb, r.args, WaterRegion.ARGS_WW_DRAW);
			}
		}
		GraphicsProgram.end(cb);
		Vk.barrier(cb);

		GpuTextureView shadowView = ShaderRenderer.shadowDepth();
		Desc shadowTex = shadowView instanceof VulkanGpuTextureView vulkan ? Desc.tex(vulkan.vkImageView(), this.nearest.handle) : depthTex;
		GraphicsProgram composite = this.composite(this.colorFormat);
		GraphicsProgram.begin(cb, w, h, mainColorView, null, mainDepthView);
		Desc matTex = Desc.tex(this.materialId, this.nearest.handle);
		for (Material material : new Material[]{Material.WATER, Material.LAVA, Material.SLIME}) {
			Desc ubo = null;
			for (Body body : bodies) {
				if (body.region().material == material) {
					ubo = body.ubo();
					break;
				}
			}
			if (ubo == null) {
				continue;
			}
			composite.bind(cb, ubo,
				Desc.tex(this.gbuffer, this.nearest.handle),
				Desc.tex(this.thickness, this.linear.handle),
				Desc.tex(this.sceneColor, this.linear.handle),
				depthTex,
				Desc.tex(this.foam, this.linear.handle),
				matTex,
				view,
				shadowTex,
				Desc.ssbo(this.slimeTexture),
				Desc.tex(lights.lightmap(), this.linear.handle),
				Desc.tex(lights.colorA(), this.linear.handle),
				Desc.tex(lights.colorB(), this.linear.handle),
				Desc.tex(lights.colorTint(), this.linear.handle));
			composite.push(cb, Program.f(material.shaderId()));
			composite.draw(cb, 3, 1);
		}
		GraphicsProgram.end(cb);
	}

	@Override
	public void close() {
		this.releaseTargets();
		this.composites.values().forEach(GraphicsProgram::close);
		for (AutoCloseable c : new AutoCloseable[]{
			this.meshFront, this.dropletFront, this.meshThickness, this.dropletThickness, this.whitewaterProgram, this.dropletCull,
			this.nearest, this.linear, this.slimeTexture
		}) {
			try {
				c.close();
			} catch (Exception ignored) {
			}
		}
	}
}
