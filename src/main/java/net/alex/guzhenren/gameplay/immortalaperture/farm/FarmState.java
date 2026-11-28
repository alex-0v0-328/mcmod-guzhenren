package net.alex.guzhenren.gameplay.immortalaperture.farm;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地种植的经营状态：标准田（各带作物与田型）、规划队列、上一轮托管的结果（托管有主就跑，没有开关，Alex 定）。
 * <ul>
 *   <li>标准田记 {@link FieldPlot}，顺序即开垦顺序；规划队列记区块坐标（{@code ChunkPos#toLong}），先进先出，田加规划
 *       合计受 {@code Siting#FIELD_CAP} 限制（在 {@link FieldPlans} 查）。</li>
 *   <li>作物都是 {@link FarmRules#crops()} 的下标，{@value #NO_CROP} = 未选。新田一律未选、普通田，作物由主人在地图上
 *       选定后种植（Alex 定），清除作物时重置为未选。搬迁的田先进规划队列，原来的作物记在规划上
 *       （{@link #planCrop}），开垦好就沿用。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code farm}），任何改动经回调置脏；开面板补种轮到的田序
 *       {@link #topUpCursor} 不存档。</li>
 * </ul>
 */

public final class FarmState implements LandSavePart.State {

    public static final LandSavePart<FarmState> PART = new LandSavePart<>("farm", FarmState::new);
    public static final int NO_CROP = -1;
    private final Runnable changed;
    private final List<FieldPlot> fields = new ArrayList<>();
    private final Map<FieldPlot, Integer> fieldCrops = new HashMap<>();
    private final Map<FieldPlot, FieldKind> fieldKinds = new HashMap<>();
    private final LongList plans = new LongArrayList();
    private final Long2IntMap planCrops = new Long2IntOpenHashMap();
    private int lastReaped;
    private int lastSown;
    private int topUpCursor;

    public FarmState(@NotNull Runnable changed) {
        this.changed = changed;
        planCrops.defaultReturnValue(NO_CROP);
    }

    public static @NotNull FarmState of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public static int clampCrop(int crop) { return crop < 0 || crop >= FarmRules.crops().size() ? NO_CROP : crop; }

    //region Fields [田与规划]
    public @NotNull List<FieldPlot> fields() { return List.copyOf(fields); }

    public void addField(@NotNull FieldPlot field) {
        fields.add(field);
        fieldCrops.put(field, NO_CROP);
        changed.run();
    }

    public void removeField(@NotNull FieldPlot field) {
        if (!fields.remove(field)) return;
        fieldCrops.remove(field);
        fieldKinds.remove(field);
        changed.run();
    }

    public @Nullable FieldPlot fieldAt(int chunkX, int chunkZ) {
        for (FieldPlot field : fields) {
            if (field.chunk().x == chunkX && field.chunk().z == chunkZ) return field;
        }
        return null;
    }

    public int crop(@NotNull FieldPlot field) { return fieldCrops.getOrDefault(field, NO_CROP); }

    public void setCrop(@NotNull FieldPlot field, int crop) {
        int clamped = clampCrop(crop);
        if (!fields.contains(field) || crop(field) == clamped) return;
        fieldCrops.put(field, clamped);
        changed.run();
    }

    public @NotNull FieldKind kind(@NotNull FieldPlot field) { return fieldKinds.getOrDefault(field, FieldKind.PLAIN); }

    public void setKind(@NotNull FieldPlot field, @NotNull FieldKind kind) {
        if (!fields.contains(field) || kind(field) == kind) return;
        fieldKinds.put(field, kind);
        changed.run();
    }

    public @NotNull LongList plans() { return LongList.of(plans.toLongArray()); }

    public void addPlan(long chunk) {
        plans.add(chunk);
        changed.run();
    }

    public boolean removePlan(long chunk) {
        if (!plans.rem(chunk)) return false;
        planCrops.remove(chunk);
        changed.run();
        return true;
    }

    public int planCrop(long chunk) { return planCrops.get(chunk); }

    public void setPlanCrop(long chunk, int crop) {
        int clamped = clampCrop(crop);
        if (!plans.contains(chunk) || planCrop(chunk) == clamped) return;
        if (clamped == NO_CROP) planCrops.remove(chunk);
        else planCrops.put(chunk, clamped);
        changed.run();
    }

    public int planned() { return fields.size() + plans.size(); }
    //endregion

    public int topUpCursor() { return topUpCursor; }

    public void setTopUpCursor(int cursor) { topUpCursor = cursor; }

    public int lastReaped() { return lastReaped; }

    public int lastSown() { return lastSown; }

    public void setLastRound(int reaped, int sown) {
        lastReaped = reaped;
        lastSown = sown;
        changed.run();
    }

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = new CompoundTag();
        tag.put("fields", new LongArrayTag(fields.stream().mapToLong(FieldPlot::origin).toArray()));
        tag.put("field_sizes", new IntArrayTag(fields.stream().mapToInt(FieldPlot::size).toArray()));
        tag.put("field_crops", new IntArrayTag(fields.stream().mapToInt(this::crop).toArray()));
        tag.put("field_kinds", new IntArrayTag(fields.stream().mapToInt(field -> kind(field).ordinal()).toArray()));
        tag.put("plans", new LongArrayTag(plans.toLongArray()));
        tag.put("plan_crops", new IntArrayTag(plans.longStream().mapToInt(this::planCrop).toArray()));
        tag.putInt("last_reaped", lastReaped);
        tag.putInt("last_sown", lastSown);
        parent.put(key, tag);
    }

    @Override
    public void load(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = parent.getCompound(key);
        long[] origins = tag.getLongArray("fields");
        int[] sizes = tag.getIntArray("field_sizes");
        int[] crops = tag.getIntArray("field_crops");
        int[] kinds = tag.getIntArray("field_kinds");
        for (int i = 0; i < origins.length && i < sizes.length; i++) {
            FieldPlot field = FieldPlot.load(origins[i], sizes[i]);
            fields.add(field);
            fieldCrops.put(field, i < crops.length ? clampCrop(crops[i]) : NO_CROP);
            if (i < kinds.length) fieldKinds.put(field, FieldKind.byId(kinds[i]));
        }
        long[] planned = tag.getLongArray("plans");
        int[] planCropIds = tag.getIntArray("plan_crops");
        for (int i = 0; i < planned.length; i++) {
            plans.add(planned[i]);
            int crop = i < planCropIds.length ? clampCrop(planCropIds[i]) : NO_CROP;
            if (crop != NO_CROP) planCrops.put(planned[i], crop);
        }
        lastReaped = tag.getInt("last_reaped");
        lastSown = tag.getInt("last_sown");
    }
}
