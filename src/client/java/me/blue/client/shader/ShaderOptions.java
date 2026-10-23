package me.blue.client.shader;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import me.blue.RealisticWater;
import me.blue.client.shader.pack.ShaderMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class ShaderOptions {
	public static final OptionInstance<Boolean> SHADERS = new OptionInstance<>("blues-api.options.shaders", value -> shadersTooltip(), (caption, value) -> caption,
			new OptionInstance.ValueSet<>() {
				@Override
				public Function<OptionInstance<Boolean>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<Boolean> tooltip, Options options, int x, int y,
						int width, OptionInstance.ValueUpdateListener<? super Boolean> onValueChanged) {
					return option -> {
						Button button = Button.builder(Component.translatable("blues-api.options.shaders.button"), pressed -> openMenu())
								.bounds(x, y, width, 20)
								.tooltip(shadersTooltip())
								.build();
						button.active = ShaderRenderer.vulkan();
						return button;
					};
				}

				@Override
				public Optional<Boolean> validateValue(Boolean value) {
					return Optional.of(value);
				}

				@Override
				public Codec<Boolean> codec() {
					return Codec.BOOL;
				}
			}, true, value -> {
			});

	private ShaderOptions() {
	}

	public static boolean clientShaderAllowed() {
		return true;
	}

	public static Tooltip shadersTooltip() {
		return Tooltip.create(Component.translatable(ShaderRenderer.vulkan() ? "blues-api.options.shaders.tooltip" : "blues-api.options.shaders.needsVulkan"));
	}

	private static void openMenu() {
		if (!ShaderRenderer.vulkan()) {
			return;
		}

		ShaderMenu.open(Minecraft.getInstance().gui.screen());
	}

	public static void processOptions(Object access) {
		try {
			Method strings = process(access, String.class);
			String selected = ShaderPacks.selectedForOptions();
			String read = (String) strings.invoke(access, "realistic_water_shader", selected == null ? ShaderPacks.UNSET : selected);
			ShaderPacks.loadSelected(read.equals(ShaderPacks.UNSET) ? "" : read);
		} catch (ReflectiveOperationException | RuntimeException e) {
			RealisticWater.LOGGER.error("Couldn't load or save the shader settings", e);
		}
	}

	private static Method process(Object access, Class<?> type) throws NoSuchMethodException {
		for (Class<?> face : access.getClass().getInterfaces()) {
			try {
				Method method = face.getMethod("process", String.class, type);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException ignored) {
			}
		}

		throw new NoSuchMethodException("process(String, " + type + ") on " + access.getClass());
	}

	static void playerPicked() {
	}

	static void sendState(boolean byPlayer) {
	}
}
