package net.alex.guzhenren.datagen.loot;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.RemoveItemLootModifier;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import yesman.epicfight.registry.entries.EpicFightItems;

/**
 * Writes the global loot modifiers: a vanilla Blue Orchid broken with a hoe also drops one Moon Orchid Petals
 * [月兰花瓣], on top of the flower itself; {@code no_skill_books} strips Epic Fight's skill books from all loot.
 *
 * <p>The petals ride NeoForge's {@code neoforge:add_table}: the modifier matches the target table and the tool, then
 * rolls the small table {@link Subtables} writes. The skill books go through this mod's {@code RemoveItemLootModifier}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModLootModifierProvider extends GlobalLootModifierProvider {

    private static final ResourceKey<LootTable> MOON_ORCHID_PETALS =
            ResourceKey.create(Registries.LOOT_TABLE, Guzhenren.id("modifiers/moon_orchid_petals"));

    public ModLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Guzhenren.MOD_ID);
    }

    @Override
    protected void start() {
        add("moon_orchid_petals", new AddTableLootModifier(new LootItemCondition[] {
                LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("blocks/blue_orchid")).build(),
                ModBlockLootProvider.hasHoe().build() }, MOON_ORCHID_PETALS));
        add("no_skill_books", new RemoveItemLootModifier(new LootItemCondition[0], EpicFightItems.SKILLBOOK));
    }

    public static final class Subtables implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(MOON_ORCHID_PETALS, LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(ModItems.MOON_ORCHID_PETALS.get()))));
        }
    }
}
