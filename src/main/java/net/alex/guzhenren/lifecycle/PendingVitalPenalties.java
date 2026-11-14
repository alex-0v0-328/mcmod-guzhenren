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
 * 主人离线时死去、待其下次登录结算的本命蛊[Vital Gu]。
 * <ul>
 *   <li>离线玩家附件写不进，丢失的物品栈暂存主世界数据存储，待 {@link VitalLossService} 按在线同等惩罚结算。</li>
 *   <li>⚠ 每蛊一条、先进先出；每次心跳结算一条，免得第二次 80% 伤害被第一次的受伤冷却吞掉。</li>
 *   <li>⚠ 模组唯一存玩家状态的服务端级存储、唯一可变的玩家状态类；其余玩家状态皆不可变附件记录（其他服务端级存储存世界
 *       状态：进行中的压力爆炸坑、{@code EpicFightRuleSeed}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see VitalLossService
 * @since 1.0.0
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
