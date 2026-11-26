package net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * 福地荡魂山页的经营状态：「挪生物上山」何时可再用（游戏刻，读档后仍有效）。
 */

public final class SoulsunderMountainState {

    private final Runnable changed;
    private long moveReadyAt;

    public SoulsunderMountainState(@NotNull Runnable changed) { this.changed = changed; }

    public long moveReadyAt() { return moveReadyAt; }

    public void setMoveReadyAt(long gameTime) {
        moveReadyAt = gameTime;
        changed.run();
    }

    public @NotNull CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("move_ready_at", moveReadyAt);
        return tag;
    }

    public void load(@NotNull CompoundTag tag) { moveReadyAt = tag.getLong("move_ready_at"); }
}
