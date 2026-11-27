package net.alex.guzhenren.gameplay.immortalaperture.farm;

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
 * 福地种植的经营状态：标准田（各带作物与田型）、规划队列、托管开关、新田作物、上一轮托管的结果。
 * <ul>
 *   <li>标准田记 {@link FieldPlot}，顺序即开垦顺序；规划队列记区块坐标（{@code ChunkPos#toLong}），先进先出，田加规划
 *       合计受 {@code Siting#FIELD_CAP} 限制（在 {@link FieldPlans} 查）。</li>
 *   <li>作物都是 {@link FarmRules#crops()} 的下标，{@value #AUTO_CROP} = 自动。新田作物是开垦时给新田的作物，田的作物
 *       随后各改各的；新田一律普通田。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code farm}），任何改动经回调置脏。</li>
 * </ul>
 */

public final class FarmState implements LandSavePart.State {

    public static final LandSavePart<FarmState> PART = new LandSavePart<>("farm", FarmState::new);
    public static final int AUTO_CROP = -1;
    private final Runnable changed;
    private final List<FieldPlot> fields = new ArrayList<>();
    private final Map<FieldPlot, Integer> fieldCrops = new HashMap<>();
    private final Map<FieldPlot, FieldKind> fieldKinds = new HashMap<>();
    private final LongList plans = new LongArrayList();
    private boolean hosting;
    private int crop = AUTO_CROP;
    private int lastReaped;
    private int lastSown;

    public FarmState(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull FarmState of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public static int clampCrop(int crop) { return crop < 0 || crop >= FarmRules.crops().size() ? AUTO_CROP : crop; }

    //region Fields [田与规划]
    public @NotNull List<FieldPlot> fields() { return List.copyOf(fields); }

    public void addField(@NotNull FieldPlot field) {
        fields.add(field);
        fieldCrops.put(field, crop);
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

    public int crop(@NotNull FieldPlot field) { return fieldCrops.getOrDefault(field, AUTO_CROP); }

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
        changed.run();
        return true;
    }

    public int planned() { return fields.size() + plans.size(); }
    //endregion

    public boolean hosting() { return hosting; }

    public void setHosting(boolean on) {
        if (hosting == on) return;
        hosting = on;
        changed.run();
    }

    public int crop() { return crop; }

    public void setCrop(int crop) {
        int clamped = clampCrop(crop);
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

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = new CompoundTag();
        tag.put("fields", new LongArrayTag(fields.stream().mapToLong(FieldPlot::origin).toArray()));
        tag.put("field_sizes", new IntArrayTag(fields.stream().mapToInt(FieldPlot::size).toArray()));
        tag.put("field_crops", new IntArrayTag(fields.stream().mapToInt(this::crop).toArray()));
        tag.put("field_kinds", new IntArrayTag(fields.stream().mapToInt(field -> kind(field).ordinal()).toArray()));
        tag.put("plans", new LongArrayTag(plans.toLongArray()));
        tag.putBoolean("hosting", hosting);
        tag.putInt("crop", crop);
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
            fieldCrops.put(field, i < crops.length ? clampCrop(crops[i]) : AUTO_CROP);
            if (i < kinds.length) fieldKinds.put(field, FieldKind.byId(kinds[i]));
        }
        for (long plan : tag.getLongArray("plans")) plans.add(plan);
        hosting = tag.getBoolean("hosting");
        crop = tag.contains("crop") ? clampCrop(tag.getInt("crop")) : AUTO_CROP;
        lastReaped = tag.getInt("last_reaped");
        lastSown = tag.getInt("last_sown");
    }
}
