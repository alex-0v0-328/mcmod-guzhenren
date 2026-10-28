package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.DecorativeHopperBlock;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every block in the mod. The properties line copies vanilla water block-for-block (replaceable,
 * no collision, 100 blast resistance, destroyed by pistons, no loot table) so the Spirit Spring
 * [元泉] feels native, plus lava's full 15 light level -- Alex's pick: the spring is
 * a glowstone-bright safe zone where monsters cannot spawn. The BlockItem is registered in
 * {@link net.alex.guzhenren.registry.item.ModItems} so the creative tab's single item walk
 * still sees it.
 *
 * <p>In {@link #SPIRIT_SPRING}, it is safe to call {@code get()} on
 * {@link net.alex.guzhenren.registry.fluid.ModFluids#SPIRIT_SPRING} here: vanilla registers FLUID
 * before BLOCK, so the holder is filled.
 *
 * <p>{@link #PURE_WATER} is the Pure Water [纯水] liquid block: a plain {@code LiquidBlock} on the same water
 * properties line, without the spring's light. It has no BlockItem; the Pure Water Bucket [纯水桶] carries it.
 *
 * <p>{@link #NINE_LEAF_VITALITY_GRASS} is the Nine Leaf Vitality Grass [九叶生机草] growing in the world: a
 * dandelion-shaped flower with no BlockItem, whose loot table drops the wild Gu item of the same id. Its
 * suspicious-stew effect is dormant -- no flower tag and no BlockItem carry it anywhere.
 *
 * <p>{@link #INTIMATE_GRASS} is the two-block Intimate Grass [知心草], vanilla tall grass in shape, properties and
 * biome tint, without a BlockItem; its loot table gives an Intimate Grass Bundle [知心草束] to a hoe only.
 *
 * <p>{@link #STEEL_BLOCK} is the Steel Block [钢块]: a plain storage block on the iron block's properties line, so
 * it mines, sounds and serves a beacon like one; nine Steel Ingots [钢锭] craft it and it crafts back.
 *
 * <p>{@link #DECORATIVE_HOPPER} is the structure-template stand-in for vanilla hoppers: hopper properties,
 * no BlockItem and no loot, so mining it yields nothing; builders place hoppers and swap them in with
 * {@code /fill ... replace}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guzhenren.MOD_ID);
    public static final DeferredBlock<SpiritSpringBlock> SPIRIT_SPRING = BLOCKS.register("spirit_spring",
            () -> new SpiritSpringBlock(ModFluids.SPIRIT_SPRING.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .lightLevel(state -> 15)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));
    public static final DeferredBlock<LiquidBlock> PURE_WATER = BLOCKS.register("pure_water",
            () -> new LiquidBlock(ModFluids.PURE_WATER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));
    public static final DeferredBlock<FlowerBlock> NINE_LEAF_VITALITY_GRASS = BLOCKS.register(
            "nine_leaf_vitality_grass",
            () -> new FlowerBlock(MobEffects.REGENERATION, 8.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<DoublePlantBlock> INTIMATE_GRASS = BLOCKS.register("intimate_grass",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)));

    public static final DeferredBlock<DecorativeHopperBlock> DECORATIVE_HOPPER = BLOCKS.register("decorative_hopper",
            () -> new DecorativeHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).noLootTable()));
    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }
}
