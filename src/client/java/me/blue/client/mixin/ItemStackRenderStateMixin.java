package me.blue.client.mixin;

import me.blue.client.shader.EntityMaterials;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.Item;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements EntityMaterials.ItemHolder {
	@Unique
	private @Nullable Item blue$item;

	@Override
	public @Nullable Item blue$item() {
		return blue$item;
	}

	@Override
	public void blue$setItem(@Nullable Item item) {
		blue$item = item;
	}

	@ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int blue$tagItem(int overlayCoords) {
		return EntityMaterials.tagItem(overlayCoords, blue$item);
	}
}
