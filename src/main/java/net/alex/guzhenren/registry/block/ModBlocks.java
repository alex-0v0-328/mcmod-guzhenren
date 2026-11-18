package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.BlackSoilFarmlandBlock;
import net.alex.guzhenren.block.IntimateGrassCropBlock;
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
 * 本模组全部方块注册表。
 * <ul>
 *   <li>{@link #SPIRIT_SPRING}（元泉[Spirit Spring]）：逐项沿用原版水属性（可替换、无碰撞、爆炸抗性 100、活塞推毁、无战
 *       利品表）并附加满级 15 级光照，{@code BlockItem} 在 {@link net.alex.guzhenren.registry.item.ModItems} 注册（此处
 *       直接读取 {@link ModFluids#SPIRIT_SPRING} 是安全的，因流体注册先于方块执行）。</li>
 *   <li>{@link #PURE_WATER}：与原版水同属性的普通 {@code LiquidBlock}，无光照且不单独提供 {@code BlockItem}，由纯水桶放
 *       置。</li>
 *   <li>{@link #NINE_LEAF_VITALITY_GRASS_3}（野生九叶生机草[Nine Leaf Vitality Grass]）：采用蒲公英外形与属性的
 *       {@link FlowerBlock}，无 {@code BlockItem} 也不加入花朵标签，破坏时掉落同 ID 野生蛊物品（谜之炖菜效果仅作构造占
 *       位而不对外提供）。</li>
 *   <li>{@link #INTIMATE_GRASS} 与 {@link #INTIMATE_GRASS_CROP}（知心草及其作物）：前者为两格高草丛（贴图自带色彩、不随
 *       群系染色，无 {@code BlockItem}，无需工具即可掉落草束与种子，由 Gu World 生成），后者沿用小麦属性并由知心草种子
 *       种植（{@link IntimateGrassCropBlock}）。</li>
 *   <li>{@link #BLACK_SOIL_FARMLAND} 与 {@link #STEEL_BLOCK}：黑土耕地（{@link BlackSoilFarmlandBlock}）沿用原版耕地属
 *       性并由腐泥冻土右键转化（挖毁掉落泥土，{@code BlockItem} 仅进创造栏）；钢块则沿用铁块属性与 9 个钢锭互合。</li>
 * </ul>
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
    public static final DeferredBlock<IntimateGrassCropBlock> INTIMATE_GRASS_CROP = BLOCKS.register(
            "intimate_grass_crop",
            () -> new IntimateGrassCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredBlock<BlackSoilFarmlandBlock> BLACK_SOIL_FARMLAND = BLOCKS.register(
            "black_soil_farmland",
            () -> new BlackSoilFarmlandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND)));

    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }
}
