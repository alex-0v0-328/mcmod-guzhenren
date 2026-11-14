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
import org.jetbrains.annotations.NotNull;

/**
 * 全局战利品修改器：删去生成战利品中某物品的全部堆叠，其余不动；实例由 {@code ModLootModifierProvider} 写出，
 * {@code guzhenren:no_skill_books} 删 Epic Fight 技能书。
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
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot,
                                                          @NotNull LootContext context) {
        generatedLoot.removeIf(stack -> stack.is(item));
        return generatedLoot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.REMOVE_ITEM.get();
    }
}
