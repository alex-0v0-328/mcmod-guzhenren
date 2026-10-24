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
 * Vital Gu [本命蛊] that died while their owner was offline, waiting for the owner's next login. An
 * offline player's attachments cannot be written, so the lost stack waits here, in the
 * overworld's data storage, until {@link VitalLossService} charges it
 * through the same penalty an online owner pays.
 *
 * <p>⚠ The mod's only server-level store of player state and its one mutable player-state class -- every
 * other piece of player state is an immutable attachment record (the other server-level store, the running
 * pressure-explosion craters, holds world state). ⚠ One entry per lost Gu, first in first out; the
 * service settles one per heartbeat, so the second 80% hurt is not swallowed by the first one's hurt
 * cooldown.
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
