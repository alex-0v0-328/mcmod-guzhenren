package net.alex.guzhenren.datagen.loot;

import java.util.Set;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组方块的战利品表子生成器。
 * <ul>
 *   <li>{@link #getKnownBlocks} 遍历本模组注册的全部方块（流体方块无战利品表），确保新增有形方块漏写战利品表时在
 *       {@code runData} 阶段直接报错；三转九叶生机草[Nine Leaf Vitality Grass]直接掉落满叶状态的野生蛊物品，黑土耕地掉
 *       落泥土，钢块掉落自身。</li>
 *   <li>野生知心草与知心草作物均掉落知心草束与知心草种子（种子数量应用原版小麦的时运二项分布加成与爆炸衰减）；
 *       {@link #hasHoe} 提供锄头工具匹配条件供 {@link ModLootModifierProvider} 复用。</li>
 *   <li>⚠ 野生知心草作为双层植物（{@link DoublePlantBlock}），在一半被破坏时另一半会因失去支撑紧接着破坏并各自结算一次
 *       战利品表；因此必须通过 {@link #isHalfWithOtherHalf} 限制仅当对应另一半方块尚存时才掉落战利品，确保无论破坏上半
 *       格还是下半格都只掉落一份。</li>
 * </ul>
 */

public final class ModBlockLootProvider extends BlockLootSubProvider {

    private static final int WILD_BUNDLES = 2;
    private static final int WILD_SEED_ROLLS = 3;
    private static final int CROP_SEED_ROLLS = 1;
    private static final float SEED_ROLL_CHANCE = 0.5714286F;

    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    static LootItemCondition.Builder hasHoe() {
        return MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.HOES));
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }

    @Override
    protected void generate() {
        dropOther(ModBlocks.NINE_LEAF_VITALITY_GRASS_3.get(), ModItems.NINE_LEAF_VITALITY_GRASS_3.get());
        add(ModBlocks.INTIMATE_GRASS.get(), this::createWildIntimateGrassDrops);
        add(ModBlocks.INTIMATE_GRASS_CROP.get(), this::createIntimateGrassCropDrops);
        dropOther(ModBlocks.BLACK_SOIL_FARMLAND.get(), Blocks.DIRT);
        dropSelf(ModBlocks.STEEL_BLOCK.get());
    }

    //region Intimate grass [知心草]
    private LootTable.Builder createWildIntimateGrassDrops(Block block) {
        LootItemCondition.Builder intact = AnyOfCondition.anyOf(
                isHalfWithOtherHalf(block, DoubleBlockHalf.LOWER, DoubleBlockHalf.UPPER, 1),
                isHalfWithOtherHalf(block, DoubleBlockHalf.UPPER, DoubleBlockHalf.LOWER, -1));
        return applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().when(intact)
                        .add(LootItem.lootTableItem(ModItems.INTIMATE_GRASS_BUNDLE.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(WILD_BUNDLES)))))
                .withPool(LootPool.lootPool().when(intact)
                        .add(createSeeds(WILD_SEED_ROLLS))));
    }

    private LootTable.Builder createIntimateGrassCropDrops(Block block) {
        LootItemCondition.Builder mature = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties()
                        .hasProperty(CropBlock.AGE, CropBlock.MAX_AGE));
        return applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.INTIMATE_GRASS_BUNDLE.get()).when(mature)
                                .otherwise(LootItem.lootTableItem(ModItems.INTIMATE_GRASS_SEEDS.get()))))
                .withPool(LootPool.lootPool().when(mature)
                        .add(createSeeds(CROP_SEED_ROLLS))));
    }

    private LootPoolEntryContainer.Builder<?> createSeeds(int extraRolls) {
        Holder<Enchantment> fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return LootItem.lootTableItem(ModItems.INTIMATE_GRASS_SEEDS.get())
                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, SEED_ROLL_CHANCE, extraRolls));
    }

    private static LootItemCondition.Builder isHalfWithOtherHalf(Block block, DoubleBlockHalf half,
                                                                 DoubleBlockHalf other, int otherOffsetY) {
        return AllOfCondition.allOf(
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(DoublePlantBlock.HALF, half)),
                LocationCheck.checkLocation(LocationPredicate.Builder.location()
                        .setBlock(BlockPredicate.Builder.block().of(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(DoublePlantBlock.HALF, other))),
                        new BlockPos(0, otherOffsetY, 0)));
    }
    //endregion
}
