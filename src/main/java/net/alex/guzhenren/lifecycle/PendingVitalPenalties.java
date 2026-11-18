package net.alex.guzhenren.lifecycle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 离线期间本命蛊损毁的待结算惩罚队列（基于主世界 {@link ModSavedData} 持久化），供 {@link VitalLossService} 在玩家重新
 * 登录后逐只补扣。
 * <ul>
 *   <li>离线玩家的实体附件无法直接写入，因此将其损毁的本命蛊物品栈暂存于本服务端存档队列，待其上线后再按在线同等规则结
 *       算。</li>
 *   <li>⚠ 按玩家 UUID 维护先进先出队列且每次心跳至多结算一只，避免第二次 80% 当前生命值伤害被第一次的受伤无敌帧吞
 *       掉。</li>
 *   <li>⚠ 本类是全模组唯一用于保存玩家跨线状态的服务端级存储及唯一可变玩家状态容器（其余服务端存储仅存世界状态，如爆炸
 *       坑与 {@code EpicFightRuleSeed}，其余玩家状态均为不可变附件记录）。</li>
 * </ul>
 */

public final class PendingVitalPenalties extends ModSavedData {

    private static final String NAME = "guzhenren_pending_vital_penalties";
    private final Map<UUID, Deque<ItemStack>> pending = new HashMap<>();

    public static @NotNull PendingVitalPenalties get(@NotNull MinecraftServer server) {
        return ModSavedData.get(server.overworld(), NAME, PendingVitalPenalties::new, PendingVitalPenalties::load);
    }

    public void recordPenalty(@NotNull UUID owner, @NotNull ItemStack stack) {
        if (stack.isEmpty()) return;

        pending.computeIfAbsent(owner, key -> new ArrayDeque<>()).addLast(stack.copy());
        setDirty();
    }

    public int count(@NotNull UUID owner) {
        Deque<ItemStack> queue = pending.get(owner);
        return queue == null ? 0 : queue.size();
    }

    public @Nullable ItemStack poll(@NotNull UUID owner) {
        Deque<ItemStack> queue = pending.get(owner);
        if (queue == null) return null;

        ItemStack next = queue.pollFirst();
        if (queue.isEmpty()) pending.remove(owner);
        setDirty();
        return next;
    }

    @Override
    protected void write(@NotNull CompoundTag tag, @NotNull StackIo stacks) {
        ListTag entries = new ListTag();
        pending.forEach((owner, queue) -> {
            for (ItemStack stack : queue) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("owner", owner);
                entry.put("stack", stacks.save(stack));
                entries.add(entry);
            }
        });
        tag.put("pending", entries);
    }

    public static @NotNull PendingVitalPenalties load(@NotNull CompoundTag tag, @NotNull StackIo stacks) {
        PendingVitalPenalties data = new PendingVitalPenalties();
        ListTag entries = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ItemStack stack = stacks.load(entry.getCompound("stack"));
            if (stack.isEmpty() || !entry.hasUUID("owner")) continue;

            data.pending.computeIfAbsent(entry.getUUID("owner"), key -> new ArrayDeque<>()).addLast(stack);
        }
        return data;
    }
}
