package net.alex.guzhenren.datagen.loot;

import java.util.Set;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
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
 *   <li>知心草仅用锄时掉 1 个知心草束；故意不加 {@code half} 条件：被破坏那半经
 *       {@code DoublePlantBlock.playerWillDestroy} 按玩家工具掉，另一半无工具掉，锄敲哪半都正好一束。</li>
 * </ul>
 */

public final class ModBlockLootProvider extends BlockLootSubProvider {

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
        add(ModBlocks.INTIMATE_GRASS.get(), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModItems.INTIMATE_GRASS_BUNDLE.get()).when(hasHoe()))));
        dropSelf(ModBlocks.STEEL_BLOCK.get());
    }
}
