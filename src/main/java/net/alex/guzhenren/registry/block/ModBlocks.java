package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
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
 * 本模组全部方块。
 *
 * <ul>
 *   <li>{@link #SPIRIT_SPRING} 元泉[Spirit Spring]：属性逐项照抄原版水（可替换、无碰撞、爆炸抗性 100、活塞破坏、
 *       无战利品表），手感原生；另加岩浆的满 15 级光，Alex 定为萤石般明亮、怪物不生成的安全区。</li>
 *   <li>元泉 BlockItem 在 {@link net.alex.guzhenren.registry.item.ModItems} 注册，创造栏单次遍历才见得到；此处可安全
 *       {@code get()} {@link ModFluids#SPIRIT_SPRING}，因原版先注册 FLUID。</li>
 *   <li>{@link #PURE_WATER} 纯水：同一水属性的普通 {@code LiquidBlock}，无光照、无 BlockItem，由纯水桶携带。</li>
 *   <li>{@link #NINE_LEAF_VITALITY_GRASS_3} 野外生长的九叶生机草[Nine Leaf Vitality Grass]：蒲公英形花，无 BlockItem，
 *       掉同 ID 野生蛊物品；谜之炖菜效果闲置，无花标签或 BlockItem 带出。</li>
 *   <li>{@link #INTIMATE_GRASS} 两格高知心草：形状、属性、群系染色同原版高草丛，无 BlockItem；仅锄头掉知心草束。</li>
 *   <li>{@link #STEEL_BLOCK} 钢块：铁块属性的普通存储方块，挖掘、音效、信标同铁块；与 9 钢锭互合。</li>
 * </ul>
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
    public static final DeferredBlock<FlowerBlock> NINE_LEAF_VITALITY_GRASS_3 = BLOCKS.register(
            "nine_leaf_vitality_grass_3",
            () -> new FlowerBlock(MobEffects.REGENERATION, 8.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<DoublePlantBlock> INTIMATE_GRASS = BLOCKS.register("intimate_grass",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)));

    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }
}
