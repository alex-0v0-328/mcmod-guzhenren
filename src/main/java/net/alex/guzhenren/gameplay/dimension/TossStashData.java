package net.alex.guzhenren.gameplay.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 宝黄天[Treasure Yellow Heaven]里丢弃后放不回背包的物品。任何途径离开后由 {@link TreasureYellowHeavenGuard#tick}
 * 掉在脚下；存档不同步，死亡继承，死在里面的在重生点拿到；{@link #with} 先补满同物品同组件的堆再开新堆。⚠ 物品堆是记录
 * 自有副本：{@link #with} 复制收进的堆，读取方转交前先复制。
 *
 * @author Alex
 * @version 1.0.0
 * @see TreasureYellowHeavenGuard
 * @since 1.0.0
 */

public record TossStashData(List<ItemStack> stacks) {

    public static final TossStashData DEFAULT = new TossStashData(List.of());

    public static final Codec<TossStashData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(TossStashData::stacks)
    ).apply(instance, TossStashData::new));

    public TossStashData {
        stacks = List.copyOf(stacks);
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public TossStashData with(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return this;

        ItemStack rest = stack.copy();
        List<ItemStack> held = new ArrayList<>(stacks.size() + 1);
        for (ItemStack kept : stacks) {
            ItemStack copy = kept.copy();
            if (!rest.isEmpty() && ItemStack.isSameItemSameComponents(copy, rest)) {
                int moved = Math.min(rest.getCount(), copy.getMaxStackSize() - copy.getCount());
                copy.grow(moved);
                rest.shrink(moved);
            }
            held.add(copy);
        }
        if (!rest.isEmpty()) held.add(rest);
        return new TossStashData(held);
    }
}
