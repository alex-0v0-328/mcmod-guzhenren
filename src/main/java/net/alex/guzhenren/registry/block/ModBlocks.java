package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.registry.fluid.ModFluids;
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

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }
}
