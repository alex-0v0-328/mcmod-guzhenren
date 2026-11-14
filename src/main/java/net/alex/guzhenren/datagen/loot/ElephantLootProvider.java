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
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

/** 两种象的掉落：2 至 4 张皮革（抢夺加成），不掉肉。 */

public final class ElephantLootProvider extends EntityLootSubProvider {

    public ElephantLootProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected @NotNull Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(ModEntityTypes.ELEPHANT.get(), ModEntityTypes.WHITE_ELEPHANT.get());
    }

    @Override
    public void generate() {
        getKnownEntityTypes().forEach(type -> add(type, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.LEATHER)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))));
    }
}
