package net.alex.guzhenren.datagen.loot;

import java.util.stream.Stream;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

/** Writes the wild boar's pork drop, including vanilla fire and looting behavior. */

public final class WildBoarLootProvider extends EntityLootSubProvider {

    public WildBoarLootProvider() {
        super(FeatureFlags.REGISTRY.allFlags());
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
                                .apply(SmeltItemFunction.smelted().when(LootItemEntityPropertyCondition
                                        .hasProperties(LootContext.EntityTarget.THIS, ENTITY_ON_FIRE)))
                                .apply(LootingEnchantFunction.lootingMultiplier(
                                        UniformGenerator.between(0.0F, 1.0F))))));
    }
}
