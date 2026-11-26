package net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain;

import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * 福地荡魂山页的经营状态：「挪生物上山」何时可再用（游戏刻，读档后仍有效）；存档段 {@link #PART}（子项 {@code mountain}）。
 */

public final class SoulsunderMountainState implements LandSavePart.State {

    public static final LandSavePart<SoulsunderMountainState> PART = new LandSavePart<>("mountain",
            SoulsunderMountainState::new);

    private final Runnable changed;
    private long moveReadyAt;

    public SoulsunderMountainState(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull SoulsunderMountainState of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public long moveReadyAt() { return moveReadyAt; }

    public void setMoveReadyAt(long gameTime) {
        moveReadyAt = gameTime;
        changed.run();
    }

    @Override
    public void save(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("move_ready_at", moveReadyAt);
        parent.put(key, tag);
    }

    @Override
    public void load(@NotNull CompoundTag parent, @NotNull String key, @NotNull StackIo stacks) {
        moveReadyAt = parent.getCompound(key).getLong("move_ready_at");
    }
}
