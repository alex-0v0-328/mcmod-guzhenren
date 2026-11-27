package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 各牧场的设置：自动收拢开关与收拢种类，按牧场西北角区块记；没设过 = 不自动收拢、种类全收。
 * <ul>
 *   <li>收拢种类记「不收」的种类：福地里新出现的种类默认就收。</li>
 *   <li>牧场列表在 {@link HerdState}，本段不跟着增删：放置时 {@link #forget} 清掉同一西北角的旧设置，迁移时
 *       {@link #move}，拆除时 {@link #forget}；割弃作废的牧场留下的旧设置只在同址再放牧场时被清掉，期间无人读。</li>
 *   <li>失主时 {@link #clearAutoGather}（同各托管开关）；收拢种类留给下一任。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code pasture_settings}），任何改动经回调置脏。</li>
 * </ul>
 */

public final class PastureSettings implements LandSavePart.State {

    public static final LandSavePart<PastureSettings> PART = new LandSavePart<>("pasture_settings",
            PastureSettings::new);
    private final Runnable changed;
    private final Set<Long> autoGather = new LinkedHashSet<>();
    private final Map<Long, Set<ResourceLocation>> excluded = new LinkedHashMap<>();

    public PastureSettings(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull PastureSettings of(@NotNull BlissfulLandData data) { return data.part(PART); }

    //region Auto gather [自动收拢]
    public boolean autoGather(@NotNull ChunkPos corner) { return autoGather.contains(corner.toLong()); }

    public void setAutoGather(@NotNull ChunkPos corner, boolean on) {
        if (on ? autoGather.add(corner.toLong()) : autoGather.remove(corner.toLong())) changed.run();
    }

    public void clearAutoGather() {
        if (autoGather.isEmpty()) return;
        autoGather.clear();
        changed.run();
    }
    //endregion

    //region Types [收拢种类]
    public @NotNull Set<ResourceLocation> excluded(@NotNull ChunkPos corner) {
        return Collections.unmodifiableSet(excluded.getOrDefault(corner.toLong(), Set.of()));
    }

    public boolean gathers(@NotNull ChunkPos corner, @NotNull ResourceLocation type) {
        return !excluded(corner).contains(type);
    }

    public void setGathers(@NotNull ChunkPos corner, @NotNull ResourceLocation type, boolean on) {
        Set<ResourceLocation> types = excluded.computeIfAbsent(corner.toLong(), key -> new LinkedHashSet<>());
        boolean edited = on ? types.remove(type) : types.add(type);
        if (types.isEmpty()) excluded.remove(corner.toLong());
        if (edited) changed.run();
    }
    //endregion

    public void move(@NotNull ChunkPos from, @NotNull ChunkPos to) {
        if (from.equals(to)) return;
        boolean auto = autoGather.remove(from.toLong());
        Set<ResourceLocation> types = excluded.remove(from.toLong());
        autoGather.remove(to.toLong());
        excluded.remove(to.toLong());
        if (auto) autoGather.add(to.toLong());
        if (types != null) excluded.put(to.toLong(), types);
        changed.run();
    }

    public void forget(@NotNull ChunkPos corner) {
        boolean auto = autoGather.remove(corner.toLong());
        if (excluded.remove(corner.toLong()) != null || auto) changed.run();
    }

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = new CompoundTag();
        tag.put("auto_gather", new LongArrayTag(autoGather.stream().mapToLong(Long::longValue).toArray()));
        ListTag list = new ListTag();
        excluded.forEach((corner, types) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("corner", corner);
            ListTag names = new ListTag();
            for (ResourceLocation type : types) names.add(StringTag.valueOf(type.toString()));
            entry.put("types", names);
            list.add(entry);
        });
        tag.put("excluded", list);
        parent.put(key, tag);
    }

    @Override
    public void load(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = parent.getCompound(key);
        for (long corner : tag.getLongArray("auto_gather")) autoGather.add(corner);
        for (Tag element : tag.getList("excluded", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            Set<ResourceLocation> types = new LinkedHashSet<>();
            for (Tag name : entry.getList("types", Tag.TAG_STRING)) {
                ResourceLocation type = ResourceLocation.tryParse(name.getAsString());
                if (type != null) types.add(type);
            }
            if (!types.isEmpty()) excluded.put(entry.getLong("corner"), types);
        }
    }
}
