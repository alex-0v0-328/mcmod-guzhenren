package net.alex.guzhenren.gameplay.rewind;

import com.mojang.serialization.Codec;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 人如故[Man As Before Gu]对持有者的身体快照环形队列附件：按由旧到新顺序保存 {@link PlayerSnapshot}（采样与选取节奏由
 * {@link RewindClock} 控制；仅服务端落盘、不同步给客户端，且死亡重生后不保留）。
 */

public record PlayerRewindData(List<PlayerSnapshot> snapshots) {

    public static final PlayerRewindData DEFAULT = new PlayerRewindData(List.of());
    public static final Codec<PlayerRewindData> CODEC = PlayerSnapshot.CODEC.listOf()
            .xmap(PlayerRewindData::new, PlayerRewindData::snapshots);

    public PlayerRewindData {
        snapshots = List.copyOf(snapshots);
    }

    public boolean isEmpty() { return snapshots.isEmpty(); }

    public long getLastTakenAt() { return RewindClock.lastTakenAt(snapshots, PlayerSnapshot::takenAt); }

    public @Nullable PlayerSnapshot pick(long now) {
        return RewindClock.pick(snapshots, PlayerSnapshot::takenAt, now);
    }

    public @NotNull PlayerRewindData with(@NotNull PlayerSnapshot next) {
        return new PlayerRewindData(RewindClock.add(snapshots, next, PlayerSnapshot::takenAt));
    }
}
