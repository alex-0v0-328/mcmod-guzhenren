package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.DecorativeHopperBlock;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
 * <p>{@link #NINE_LEAF_VITALITY_GRASS} is the Nine Leaf Vitality Grass [九叶生机草] growing in the world: a
 * dandelion-shaped flower with no BlockItem, whose loot table drops the wild Gu item of the same id. Its
 * suspicious-stew effect is dormant -- no flower tag and no BlockItem carry it anywhere.
 *
 * <p>{@link #INTIMATE_GRASS} is the two-block Intimate Grass [知心草], vanilla tall grass in shape, properties and
 * biome tint, without a BlockItem; its loot table gives an Intimate Grass Bundle [知心草束] to a hoe only.
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

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Guzhenren.MOD_ID);
    public static final RegistryObject<SpiritSpringBlock> SPIRIT_SPRING = BLOCKS.register("spirit_spring",
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
    public static final RegistryObject<FlowerBlock> NINE_LEAF_VITALITY_GRASS = BLOCKS.register(
            "nine_leaf_vitality_grass",
            () -> new FlowerBlock(() -> MobEffects.REGENERATION, 8 * Ticks.SECOND,
                    BlockBehaviour.Properties.copy(Blocks.DANDELION)));
    public static final RegistryObject<DoublePlantBlock> INTIMATE_GRASS = BLOCKS.register("intimate_grass",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.copy(Blocks.TALL_GRASS)));

    public static final RegistryObject<DecorativeHopperBlock> DECORATIVE_HOPPER = BLOCKS.register("decorative_hopper",
            () -> new DecorativeHopperBlock(BlockBehaviour.Properties.copy(Blocks.HOPPER).noLootTable()));

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }
}
