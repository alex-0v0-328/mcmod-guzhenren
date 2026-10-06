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
 * Writes the block loot tables: the Nine Leaf Vitality Grass [九叶生机草] flower drops its wild Gu item, full of
 * leaves; the Intimate Grass [知心草] gives one Intimate Grass Bundle [知心草束] to a hoe and nothing otherwise; the
 * Spirit Spring [元泉] has no table at all. The known blocks are this mod's, so a new block without a table fails
 * {@code runData}.
 *
 * <p>The Intimate Grass table has no {@code half} condition on purpose: the broken half drops through
 * {@code DoublePlantBlock.playerWillDestroy} with the player's tool, and the other half falls with no tool, so a hoe
 * yields exactly one bundle whichever half it struck.
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
        return ModBlocks.BLOCKS.getEntries().stream().<Block>map(Holder::value)::iterator;
    }

    @Override
    protected void generate() {
        dropOther(ModBlocks.NINE_LEAF_VITALITY_GRASS.get(), ModItems.NINE_LEAF_VITALITY_GRASS.get());
        add(ModBlocks.INTIMATE_GRASS.get(), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModItems.INTIMATE_GRASS_BUNDLE.get()).when(hasHoe()))));
    }
}
