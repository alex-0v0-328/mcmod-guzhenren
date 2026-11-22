package net.alex.guzhenren.gameplay.blissful.herd;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 福地养殖的经营状态：按种类的托管开关、每种保持数、上一轮托管的结果。
 * <ul>
 *   <li>保持数与托管开关分开存：关掉托管不丢保持数，一键宰杀也照保持数算；保持数 0 = 不管，不存。</li>
 *   <li>任何改动经构造时传入的回调通知所属存档置脏。</li>
 * </ul>
 */

public final class HerdState {

    private final Runnable changed;
    private final Set<ResourceLocation> hosted = new LinkedHashSet<>();
    private final Map<ResourceLocation, Integer> keep = new LinkedHashMap<>();
    private int lastSlaughtered;
    private int lastBred;

    public HerdState(@NotNull Runnable changed) { this.changed = changed; }

    public boolean hosting() { return !hosted.isEmpty(); }

    public boolean isHosted(@NotNull ResourceLocation type) { return hosted.contains(type); }

    public @NotNull Set<ResourceLocation> hosted() { return Collections.unmodifiableSet(hosted); }

    public void setHosted(@NotNull ResourceLocation type, boolean on) {
        if (on ? hosted.add(type) : hosted.remove(type)) changed.run();
    }

    public void clearHosting() {
        if (hosted.isEmpty()) return;
        hosted.clear();
        changed.run();
    }

    public int keep(@NotNull ResourceLocation type) { return keep.getOrDefault(type, 0); }

    public @NotNull Map<ResourceLocation, Integer> keep() { return Collections.unmodifiableMap(keep); }

    public void setKeep(@NotNull ResourceLocation type, int count) {
        int clamped = Math.max(0, count);
        Integer before = clamped == 0 ? keep.remove(type) : keep.put(type, clamped);
        if (before == null ? clamped != 0 : before != clamped) changed.run();
    }

    public int lastSlaughtered() { return lastSlaughtered; }

    public int lastBred() { return lastBred; }

    public void setLastRound(int slaughtered, int bred) {
        lastSlaughtered = slaughtered;
        lastBred = bred;
        changed.run();
    }

    public @NotNull CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ResourceLocation type : hosted) list.add(StringTag.valueOf(type.toString()));
        tag.put("hosted", list);
        CompoundTag counts = new CompoundTag();
        keep.forEach((type, count) -> counts.putInt(type.toString(), count));
        tag.put("keep", counts);
        tag.putInt("last_slaughtered", lastSlaughtered);
        tag.putInt("last_bred", lastBred);
        return tag;
    }

    public void load(@NotNull CompoundTag tag) {
        for (Tag element : tag.getList("hosted", Tag.TAG_STRING)) {
            ResourceLocation type = ResourceLocation.tryParse(element.getAsString());
            if (type != null) hosted.add(type);
        }
        CompoundTag counts = tag.getCompound("keep");
        for (String key : counts.getAllKeys()) {
            ResourceLocation type = ResourceLocation.tryParse(key);
            if (type != null && counts.getInt(key) > 0) keep.put(type, counts.getInt(key));
        }
        lastSlaughtered = tag.getInt("last_slaughtered");
        lastBred = tag.getInt("last_bred");
    }
}
