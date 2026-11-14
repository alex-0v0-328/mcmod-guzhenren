package net.alex.guzhenren.datagen.loot;

import java.util.stream.Stream;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

/** 野猪的猪肉掉落，含原版着火烧熟与抢夺加成。 */

public final class WildBoarLootProvider extends EntityLootSubProvider {

    public WildBoarLootProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected @NotNull Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(ModEntityTypes.WILD_BOAR.get());
    }

    @Override
    public void generate() {
        add(ModEntityTypes.WILD_BOAR.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.PORKCHOP)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(SmeltItemFunction.smelted().when(shouldSmeltLoot()))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries,
                                        UniformGenerator.between(0.0F, 1.0F))))));
    }
}
