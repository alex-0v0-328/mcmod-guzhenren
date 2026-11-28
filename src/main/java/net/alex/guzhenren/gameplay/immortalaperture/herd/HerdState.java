package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地养殖的经营状态：各牧场的西北角区块与上一轮托管的结果；每个牧场养哪种、下限多少在 {@link PastureSettings}。
 * <ul>
 *   <li>牧场按下标排列（面板操作按下标指牧场）；失主也不清：栅栏还在，下一任认主时照样挂牧场强加载
 *       （{@link PastureWork#retain}）。上限在选址时查（{@code Siting#PASTURE_CAP}）。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code herd}），任何改动经回调通知所属存档置脏。</li>
 * </ul>
 */

public final class HerdState implements LandSavePart.State {

    public static final LandSavePart<HerdState> PART = new LandSavePart<>("herd", HerdState::new);

    private final Runnable changed;
    private int lastSlaughtered;
    private int lastBred;
    private final List<ChunkPos> pastures = new ArrayList<>();

    public HerdState(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull HerdState of(@NotNull BlissfulLandData data) { return data.part(PART); }

    //region Pastures [牧场]
    public @NotNull List<ChunkPos> pastures() { return List.copyOf(pastures); }

    public @Nullable ChunkPos pasture(int index) {
        return index >= 0 && index < pastures.size() ? pastures.get(index) : null;
    }

    public void addPasture(@NotNull ChunkPos corner) {
        pastures.add(corner);
        changed.run();
    }

    public void setPasture(int index, @NotNull ChunkPos corner) {
        pastures.set(index, corner);
        changed.run();
    }

    public void removePasture(int index) {
        pastures.remove(index);
        changed.run();
    }
    //endregion

    public int lastSlaughtered() { return lastSlaughtered; }

    public int lastBred() { return lastBred; }

    public void setLastRound(int slaughtered, int bred) {
        lastSlaughtered = slaughtered;
        lastBred = bred;
        changed.run();
    }

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("last_slaughtered", lastSlaughtered);
        tag.putInt("last_bred", lastBred);
        tag.put("pastures", new LongArrayTag(pastures.stream().mapToLong(ChunkPos::toLong).toArray()));
        parent.put(key, tag);
    }

    @Override
    public void load(@NotNull CompoundTag parent, @NotNull String name, @NotNull StackIo stacks) {
        CompoundTag tag = parent.getCompound(name);
        lastSlaughtered = tag.getInt("last_slaughtered");
        lastBred = tag.getInt("last_bred");
        for (long corner : tag.getLongArray("pastures")) pastures.add(new ChunkPos(corner));
    }
}
