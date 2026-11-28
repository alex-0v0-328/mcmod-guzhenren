package net.alex.guzhenren.gameplay.immortalaperture.gu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 福地「蛊虫」页的经营状态：每种保持数（「捕获入库」捕到只剩这么多；0 = 不捕，不存）。蛊虫不自动捕获（Alex 定）。
 * <ul>
 *   <li>本类是存档段 {@link #PART}（子项 {@code gu}），任何改动经回调通知所属存档置脏。</li>
 * </ul>
 */

public final class GuState implements LandSavePart.State {

    public static final LandSavePart<GuState> PART = new LandSavePart<>("gu", GuState::new);

    private final Runnable changed;
    private final Map<ResourceLocation, Integer> keep = new LinkedHashMap<>();

    public GuState(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull GuState of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public int keep(@NotNull ResourceLocation type) { return keep.getOrDefault(type, 0); }

    public @NotNull Map<ResourceLocation, Integer> keep() { return Collections.unmodifiableMap(keep); }

    public void setKeep(@NotNull ResourceLocation type, int count) {
        int clamped = Math.max(0, count);
        Integer before = clamped == 0 ? keep.remove(type) : keep.put(type, clamped);
        if (before == null ? clamped != 0 : before != clamped) changed.run();
    }

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = new CompoundTag();
        CompoundTag counts = new CompoundTag();
        keep.forEach((type, count) -> counts.putInt(type.toString(), count));
        tag.put("keep", counts);
        parent.put(key, tag);
    }

    @Override
    public void load(@NotNull CompoundTag parent, @NotNull String name, @NotNull StackIo stacks) {
        CompoundTag tag = parent.getCompound(name);
        CompoundTag counts = tag.getCompound("keep");
        for (String key : counts.getAllKeys()) {
            ResourceLocation type = ResourceLocation.tryParse(key);
            if (type != null && counts.getInt(key) > 0) keep.put(type, counts.getInt(key));
        }
    }
}
