package net.alex.guzhenren.gameplay.rewind;

import com.mojang.serialization.Codec;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 人如故[Man As Before Gu]对持有者的记忆。{@link PlayerSnapshot} 环，旧者在前，按 {@link RewindClock} 节奏；存于玩家，
 * 不同步（客户端不决定哪天被记住）；死亡不保留，新身体没有过去。
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerRewindService
 * @since 1.0.0
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
