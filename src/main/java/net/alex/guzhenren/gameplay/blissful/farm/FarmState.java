package net.alex.guzhenren.gameplay.blissful.farm;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import org.jetbrains.annotations.NotNull;

/**
 * 福地种植的经营状态：标准田、开垦冷却、托管开关、开垦作物、上一轮托管的结果。
 * <ul>
 *   <li>标准田记水坑方块坐标（即耕地那一层的田心），顺序即开垦顺序。</li>
 *   <li>开垦作物是 {@link FarmWork#CROPS} 的下标，{@value #AUTO_CROP} = 自动（库藏里第一种种子）。</li>
 *   <li>冷却按游戏刻记「何时可再用」，读档后仍有效；任何改动经构造时传入的回调通知所属存档置脏。</li>
 * </ul>
 */

public final class FarmState {

    public static final int AUTO_CROP = -1;
    private final Runnable changed;
    private final LongList fields = new LongArrayList();
    private long reclaimReadyAt;
    private boolean hosting;
    private int crop = AUTO_CROP;
    private int lastReaped;
    private int lastSown;

    public FarmState(@NotNull Runnable changed) { this.changed = changed; }

    public @NotNull LongList fields() { return LongList.of(fields.toLongArray()); }

    public boolean hasField(@NotNull BlockPos center) { return fields.contains(center.asLong()); }

    public void addField(@NotNull BlockPos center) {
        fields.add(center.asLong());
        changed.run();
    }

    public long reclaimReadyAt() { return reclaimReadyAt; }

    public void setReclaimReadyAt(long gameTime) {
        reclaimReadyAt = gameTime;
        changed.run();
    }

    public boolean hosting() { return hosting; }

    public void setHosting(boolean on) {
        if (hosting == on) return;
        hosting = on;
        changed.run();
    }

    public int crop() { return crop; }

    public void setCrop(int crop) {
        int clamped = crop < 0 || crop >= FarmWork.CROPS.size() ? AUTO_CROP : crop;
        if (this.crop == clamped) return;
        this.crop = clamped;
        changed.run();
    }

    public int lastReaped() { return lastReaped; }

    public int lastSown() { return lastSown; }

    public void setLastRound(int reaped, int sown) {
        lastReaped = reaped;
        lastSown = sown;
        changed.run();
    }

    public @NotNull CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("fields", new LongArrayTag(fields.toLongArray()));
        tag.putLong("reclaim_ready_at", reclaimReadyAt);
        tag.putBoolean("hosting", hosting);
        tag.putInt("crop", crop);
        tag.putInt("last_reaped", lastReaped);
        tag.putInt("last_sown", lastSown);
        return tag;
    }

    public void load(@NotNull CompoundTag tag) {
        for (long field : tag.getLongArray("fields")) fields.add(field);
        reclaimReadyAt = tag.getLong("reclaim_ready_at");
        hosting = tag.getBoolean("hosting");
        crop = tag.contains("crop") ? tag.getInt("crop") : AUTO_CROP;
        if (crop < AUTO_CROP || crop >= FarmWork.CROPS.size()) crop = AUTO_CROP;
        lastReaped = tag.getInt("last_reaped");
        lastSown = tag.getInt("last_sown");
    }
}
