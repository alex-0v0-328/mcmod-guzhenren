package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.LinkedHashMap;
import java.util.Map;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 各牧场的设置：指定的动物（一个牧场只养一种，Alex 定）与下限，按牧场西北角区块记；没设过 = 未指定、下限 0。
 * <ul>
 *   <li>下限只对指定了动物的牧场有意义：托管每轮把多出下限的成年动物宰掉、不足下限的用库藏饲料喂养补上；下限 0 = 不动
 *       （防一键宰光）。换动物时下限清零。</li>
 *   <li>牧场列表在 {@link HerdState}，本段不跟着增删：放置时 {@link #forget} 清掉同一西北角的旧设置，迁移时
 *       {@link #move}，拆除时 {@link #forget}；割弃作废的牧场留下的旧设置只在同址再放牧场时被清掉，期间无人读。</li>
 *   <li>失主不清，留给下一任（托管只在有主时跑）。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code pasture_settings}），任何改动经回调置脏。</li>
 * </ul>
 */

public final class PastureSettings implements LandSavePart.State {

    public static final LandSavePart<PastureSettings> PART = new LandSavePart<>("pasture_settings",
            PastureSettings::new);
    private final Runnable changed;
    private final Map<Long, ResourceLocation> animals = new LinkedHashMap<>();
    private final Map<Long, Integer> floors = new LinkedHashMap<>();

    public PastureSettings(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull PastureSettings of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public @Nullable ResourceLocation animal(@NotNull ChunkPos corner) { return animals.get(corner.toLong()); }

    public void setAnimal(@NotNull ChunkPos corner, @Nullable ResourceLocation type) {
        ResourceLocation before = type == null ? animals.remove(corner.toLong()) : animals.put(corner.toLong(), type);
        if (type == null ? before == null : type.equals(before)) return;
        floors.remove(corner.toLong());
        changed.run();
    }

    public int floor(@NotNull ChunkPos corner) {
        return animal(corner) == null ? 0 : floors.getOrDefault(corner.toLong(), 0);
    }

    public void setFloor(@NotNull ChunkPos corner, int floor) {
        if (animal(corner) == null) return;
        int clamped = Math.max(0, floor);
        Integer before = clamped == 0 ? floors.remove(corner.toLong()) : floors.put(corner.toLong(), clamped);
        if (before == null ? clamped != 0 : before != clamped) changed.run();
    }

    public void move(@NotNull ChunkPos from, @NotNull ChunkPos to) {
        if (from.equals(to)) return;
        ResourceLocation animal = animals.remove(from.toLong());
        Integer floor = floors.remove(from.toLong());
        animals.remove(to.toLong());
        floors.remove(to.toLong());
        if (animal != null) animals.put(to.toLong(), animal);
        if (floor != null) floors.put(to.toLong(), floor);
        changed.run();
    }

    public void forget(@NotNull ChunkPos corner) {
        boolean animal = animals.remove(corner.toLong()) != null;
        if (floors.remove(corner.toLong()) != null || animal) changed.run();
    }

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        ListTag list = new ListTag();
        animals.forEach((corner, type) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("corner", corner);
            entry.putString("animal", type.toString());
            entry.putInt("floor", floors.getOrDefault(corner, 0));
            list.add(entry);
        });
        CompoundTag tag = new CompoundTag();
        tag.put("pastures", list);
        parent.put(key, tag);
    }

    @Override
    public void load(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        for (Tag element : parent.getCompound(key).getList("pastures", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            ResourceLocation type = ResourceLocation.tryParse(entry.getString("animal"));
            if (type == null) continue;
            animals.put(entry.getLong("corner"), type);
            if (entry.getInt("floor") > 0) floors.put(entry.getLong("corner"), entry.getInt("floor"));
        }
    }
}
