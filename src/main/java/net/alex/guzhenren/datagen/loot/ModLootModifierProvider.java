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
 * 本模组全局战利品修改器（{@link GlobalLootModifierProvider}）数据生成器。
 * <ul>
 *   <li>{@code moon_orchid_petals} 使用 {@link AddTableLootModifier}，在玩家用锄头
 *       （{@link ModBlockLootProvider#hasHoe}）破坏原版兰花时额外抽取子战利品表 {@link Subtables} 掉落 1 份月兰花
 *       瓣。</li>
 *   <li>{@code no_skill_books} 使用 {@link RemoveItemLootModifier} 从各类箱子与战利品表中全局剔除 Epic Fight 技能
 *       书。</li>
 * </ul>
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
