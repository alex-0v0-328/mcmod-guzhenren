package net.alex.guzhenren.datagen.loot;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * 有本模组掉落表的野兽（野猪、三种牛、两种象）的战利品表子生成器，掉落对齐原版同类动物。
 * <ul>
 *   <li>新野兽在 {@link #BEASTS} 登记、在 {@link #generate} 加一行；父类要求两处一致，漏登记会在 runData 时报错。</li>
 *   <li>每份掉落一个池、掷一次，抢夺每级加 0–1；肉类被火烧死时变熟，皮革不受影响。</li>
 * </ul>
 */
public final class BeastLootProvider extends EntityLootSubProvider {

    private static final List<Supplier<? extends EntityType<?>>> BEASTS = List.of(ModEntityTypes.WILD_BOAR,
            ModEntityTypes.YELLOW_OX, ModEntityTypes.WATER_BUFFALO, ModEntityTypes.AZURE_OX, ModEntityTypes.ELEPHANT,
            ModEntityTypes.WHITE_ELEPHANT);

    public BeastLootProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected @NotNull Stream<EntityType<?>> getKnownEntityTypes() {
        return BEASTS.stream().map(Supplier::get);
    }

    @Override
    public void generate() {
        add(ModEntityTypes.WILD_BOAR.get(), LootTable.lootTable().withPool(drop(Items.PORKCHOP, 1, 3, true)));
        Stream.of(ModEntityTypes.YELLOW_OX, ModEntityTypes.WATER_BUFFALO, ModEntityTypes.AZURE_OX).forEach(cattle ->
                add(cattle.get(), LootTable.lootTable().withPool(drop(Items.LEATHER, 0, 2, false))
                        .withPool(drop(Items.BEEF, 1, 3, true))));
        Stream.of(ModEntityTypes.ELEPHANT, ModEntityTypes.WHITE_ELEPHANT).forEach(elephant ->
                add(elephant.get(), LootTable.lootTable().withPool(drop(Items.LEATHER, 2, 4, false))));
    }

    private LootPool.Builder drop(Item item, float min, float max, boolean meat) {
        LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
        if (meat) entry.apply(SmeltItemFunction.smelted().when(shouldSmeltLoot()));
        entry.apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, 1.0F)));
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(entry);
    }
}
