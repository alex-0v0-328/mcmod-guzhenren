package net.alex.guzhenren.gameplay.immortalaperture.farm;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地种植的经营状态：标准田、规划队列、开垦冷却、托管开关、开垦作物、上一轮托管的结果。
 * <ul>
 *   <li>标准田记 {@link FieldPlot}，顺序即开垦顺序；规划队列记区块坐标（{@code ChunkPos#toLong}），先进先出，田加规划
 *       合计受 {@code Siting#FIELD_CAP} 限制（在 {@link FieldPlans} 查）。</li>
 *   <li>开垦作物是 {@link FarmWork#CROPS} 的下标，{@value #AUTO_CROP} = 自动（库藏里第一种种子）。</li>
 *   <li>冷却按游戏刻记「何时可再用」，读档后仍有效；任何改动经构造时传入的回调通知所属存档置脏。</li>
 * </ul>
 */

public final class FarmState {

    public static final int AUTO_CROP = -1;
    private final Runnable changed;
    private final List<FieldPlot> fields = new ArrayList<>();
    private final LongList plans = new LongArrayList();
    private long reclaimReadyAt;
    private boolean hosting;
    private int crop = AUTO_CROP;
    private int lastReaped;
    private int lastSown;

    public FarmState(@NotNull Runnable changed) { this.changed = changed; }

    //region Fields [田与规划]
    public @NotNull List<FieldPlot> fields() { return List.copyOf(fields); }

    public void addField(@NotNull FieldPlot field) {
        fields.add(field);
        changed.run();
    }

    public void removeField(@NotNull FieldPlot field) {
        if (fields.remove(field)) changed.run();
    }

    public @Nullable FieldPlot fieldAt(int chunkX, int chunkZ) {
        for (FieldPlot field : fields) {
            if (field.chunk().x == chunkX && field.chunk().z == chunkZ) return field;
        }
        return null;
    }

    public @NotNull LongList plans() { return LongList.of(plans.toLongArray()); }

    public void addPlan(long chunk) {
        plans.add(chunk);
        changed.run();
    }

    public boolean removePlan(long chunk) {
        if (!plans.rem(chunk)) return false;
        changed.run();
        return true;
    }

    public int planned() { return fields.size() + plans.size(); }
    //endregion

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
        tag.put("fields", new LongArrayTag(fields.stream().mapToLong(FieldPlot::origin).toArray()));
        tag.put("field_sizes", new IntArrayTag(fields.stream().mapToInt(FieldPlot::size).toArray()));
        tag.put("plans", new LongArrayTag(plans.toLongArray()));
        tag.putLong("reclaim_ready_at", reclaimReadyAt);
        tag.putBoolean("hosting", hosting);
        tag.putInt("crop", crop);
        tag.putInt("last_reaped", lastReaped);
        tag.putInt("last_sown", lastSown);
        return tag;
    }

    public void load(@NotNull CompoundTag tag) {
        long[] origins = tag.getLongArray("fields");
        int[] sizes = tag.getIntArray("field_sizes");
        for (int i = 0; i < origins.length && i < sizes.length; i++) fields.add(FieldPlot.load(origins[i], sizes[i]));
        for (long plan : tag.getLongArray("plans")) plans.add(plan);
        reclaimReadyAt = tag.getLong("reclaim_ready_at");
        hosting = tag.getBoolean("hosting");
        crop = tag.contains("crop") ? tag.getInt("crop") : AUTO_CROP;
        if (crop < AUTO_CROP || crop >= FarmWork.CROPS.size()) crop = AUTO_CROP;
        lastReaped = tag.getInt("last_reaped");
        lastSown = tag.getInt("last_sown");
    }
}
