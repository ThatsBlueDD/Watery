package me.blue;

import me.blue.block.RealisticLavaBlock;
import me.blue.block.RealisticSandBlock;
import me.blue.block.RealisticWaterBlock;
import me.blue.fluid.RealisticLavaFluid;
import me.blue.fluid.RealisticSlimeFluid;
import me.blue.fluid.RealisticWaterFluid;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModContent {
	public static final FlowingFluid FLOWING_REALISTIC_WATER = Registry.register(
		BuiltInRegistries.FLUID, RealisticWater.id("flowing_realistic_water"), new RealisticWaterFluid.Flowing());
	public static final FlowingFluid REALISTIC_WATER = Registry.register(
		BuiltInRegistries.FLUID, RealisticWater.id("realistic_water"), new RealisticWaterFluid.Source());

	public static final ResourceKey<Block> REALISTIC_WATER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, RealisticWater.id("realistic_water"));
	public static final Block REALISTIC_WATER_BLOCK = Registry.register(BuiltInRegistries.BLOCK, REALISTIC_WATER_BLOCK_KEY, new RealisticWaterBlock(
		REALISTIC_WATER,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.WATER)
			.replaceable()
			.noCollision()
			.strength(100.0F)
			.pushReaction(PushReaction.POPPED)
			.noLootTable()
			.liquid()
			.sound(SoundType.EMPTY)
			.setId(REALISTIC_WATER_BLOCK_KEY)
	));

	public static final ResourceKey<Item> REALISTIC_WATER_BUCKET_KEY = ResourceKey.create(Registries.ITEM, RealisticWater.id("realistic_water_bucket"));
	public static final Item REALISTIC_WATER_BUCKET = Registry.register(BuiltInRegistries.ITEM, REALISTIC_WATER_BUCKET_KEY, new BucketItem(
		REALISTIC_WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).setId(REALISTIC_WATER_BUCKET_KEY)));

	public static final FlowingFluid FLOWING_REALISTIC_LAVA = Registry.register(
		BuiltInRegistries.FLUID, RealisticWater.id("flowing_realistic_lava"), new RealisticLavaFluid.Flowing());
	public static final FlowingFluid REALISTIC_LAVA = Registry.register(
		BuiltInRegistries.FLUID, RealisticWater.id("realistic_lava"), new RealisticLavaFluid.Source());

	public static final ResourceKey<Block> REALISTIC_LAVA_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, RealisticWater.id("realistic_lava"));
	public static final Block REALISTIC_LAVA_BLOCK = Registry.register(BuiltInRegistries.BLOCK, REALISTIC_LAVA_BLOCK_KEY, new RealisticLavaBlock(
		REALISTIC_LAVA,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.FIRE)
			.replaceable()
			.noCollision()
			.strength(100.0F)
			.pushReaction(PushReaction.POPPED)
			.noLootTable()
			.liquid()
			.lightLevel(state -> 15)
			.sound(SoundType.EMPTY)
			.setId(REALISTIC_LAVA_BLOCK_KEY)
	));

	public static final ResourceKey<Item> REALISTIC_LAVA_BUCKET_KEY = ResourceKey.create(Registries.ITEM, RealisticWater.id("realistic_lava_bucket"));
	public static final Item REALISTIC_LAVA_BUCKET = Registry.register(BuiltInRegistries.ITEM, REALISTIC_LAVA_BUCKET_KEY, new BucketItem(
		REALISTIC_LAVA, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).setId(REALISTIC_LAVA_BUCKET_KEY)));

	public static final FlowingFluid FLOWING_REALISTIC_SLIME = Registry.register(
		BuiltInRegistries.FLUID, RealisticWater.id("flowing_realistic_slime"), new RealisticSlimeFluid.Flowing());
	public static final FlowingFluid REALISTIC_SLIME = Registry.register(
		BuiltInRegistries.FLUID, RealisticWater.id("realistic_slime"), new RealisticSlimeFluid.Source());

	public static final ResourceKey<Block> REALISTIC_SLIME_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, RealisticWater.id("realistic_slime"));
	public static final Block REALISTIC_SLIME_BLOCK = Registry.register(BuiltInRegistries.BLOCK, REALISTIC_SLIME_BLOCK_KEY, new RealisticWaterBlock(
		REALISTIC_SLIME,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.GRASS)
			.replaceable()
			.noCollision()
			.strength(100.0F)
			.pushReaction(PushReaction.POPPED)
			.noLootTable()
			.liquid()
			.sound(SoundType.EMPTY)
			.setId(REALISTIC_SLIME_BLOCK_KEY)
	));

	public static final ResourceKey<Item> REALISTIC_SLIME_BUCKET_KEY = ResourceKey.create(Registries.ITEM, RealisticWater.id("realistic_slime_bucket"));
	public static final Item REALISTIC_SLIME_BUCKET = Registry.register(BuiltInRegistries.ITEM, REALISTIC_SLIME_BUCKET_KEY, new BucketItem(
		REALISTIC_SLIME, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).setId(REALISTIC_SLIME_BUCKET_KEY)) {
		@Override
		protected void playEmptySound(final @Nullable LivingEntity user, final LevelAccessor level, final BlockPos pos) {
			level.playSound(user, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(user, GameEvent.FLUID_PLACE, pos);
		}
	});

	public static final ResourceKey<Block> REALISTIC_SAND_KEY = ResourceKey.create(Registries.BLOCK, RealisticWater.id("realistic_sand"));
	public static final Block REALISTIC_SAND = Registry.register(BuiltInRegistries.BLOCK, REALISTIC_SAND_KEY, new RealisticSandBlock(
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.SAND)
			.instrument(NoteBlockInstrument.SNARE)
			.strength(0.5F)
			.sound(SoundType.SAND)
			.noOcclusion()
			.pushReaction(PushReaction.POPPED)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos, box) -> false)
			.setId(REALISTIC_SAND_KEY)
	));

	public static final ResourceKey<Item> REALISTIC_SAND_ITEM_KEY = ResourceKey.create(Registries.ITEM, RealisticWater.id("realistic_sand"));
	public static final Item REALISTIC_SAND_ITEM = registerBlockItem(REALISTIC_SAND_ITEM_KEY, REALISTIC_SAND);

	private ModContent() {
	}

	private static Item registerBlockItem(ResourceKey<Item> key, Block block) {
		BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(key));
		item.registerBlocks(Item.BY_BLOCK, item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
			.register(output -> {
				output.insertAfter(Items.WATER_BUCKET, REALISTIC_WATER_BUCKET);
				output.insertAfter(Items.LAVA_BUCKET, REALISTIC_LAVA_BUCKET);
				output.insertAfter(REALISTIC_LAVA_BUCKET, REALISTIC_SLIME_BUCKET);
			});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
			.register(output -> output.insertAfter(Items.SAND, REALISTIC_SAND_ITEM));
	}
}
