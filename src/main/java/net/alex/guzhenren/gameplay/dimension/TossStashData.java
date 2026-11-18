package net.alex.guzhenren.gameplay.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 宝黄天[Treasure Yellow Heaven]内丢弃物品的溢出暂存附件：当玩家在宝黄天内丢弃物品且背包已满无法塞回时暂存于此，待玩家
 * 以任何途径离开宝黄天后由 {@link TreasureYellowHeavenGuard#tick} 在脚下吐出（仅服务端存档、不同步，死亡后保留以便在重
 * 生点领回）。
 * <ul>
 *   <li>{@link #with} 先尝试合并到同物品同组件的已有堆叠中，剩余部分再追加新堆。</li>
 *   <li>⚠ 内部持有的物品栈均为独立副本：{@link #with} 会拷贝传入的物品栈，外部读取方在交出或生成掉落物前也必须先拷
 *       贝。</li>
 * </ul>
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
