package net.alex.guzhenren.core;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.alex.guzhenren.registry.loot.ModLootModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * A global loot modifier that strips every stack of one item from the generated loot and leaves the rest as it was.
 *
 * <p>The instances are data, written by {@code ModLootModifierProvider}: {@code guzhenren:no_skill_books} strips
 * Epic Fight's skill books.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class RemoveItemLootModifier extends LootModifier {

    public static final MapCodec<RemoveItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(modifier -> modifier.item))
            .apply(instance, RemoveItemLootModifier::new));

    private final Holder<Item> item;

    public RemoveItemLootModifier(LootItemCondition[] conditions, Holder<Item> item) {
        super(conditions);
        this.item = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(stack -> stack.is(item));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.REMOVE_ITEM.get();
    }
}
