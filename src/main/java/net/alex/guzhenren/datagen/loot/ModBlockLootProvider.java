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
 * 方块战利品表。
 *
 * <ul>
 *   <li>九叶生机草[Nine Leaf Vitality Grass]掉其野生蛊物品，叶子全满；元泉[Spirit Spring]与纯水无表；已知方块取本模组全
 *       部方块，新方块缺表让 {@code runData} 失败。</li>
 *   <li>野生知心草任何方式破坏都掉 {@value #WILD_BUNDLES} 个知心草束，加 1 颗种子与 {@value #WILD_SEED_ROLLS} 次
 *       {@value #SEED_ROLL_CHANCE} 的额外判定（同成熟小麦的种子量，时运每级多一次）。</li>
 *   <li>⚠ 双层植物两半各滚一次表：被打那半经 {@code DoublePlantBlock.playerWillDestroy} 掉，另一半随后失去支撑再掉；
 *       故照原版双层植物只在另一半还在时掉落，打哪半都正好一份。</li>
 *   <li>知心草作物成熟掉 1 个知心草束，加 1 颗种子与 {@value #CROP_SEED_ROLLS} 次额外判定；未成熟只退 1 颗种子。</li>
 *   <li>{@link #hasHoe} 供 {@code ModLootModifierProvider} 的月兰花瓣用。</li>
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
