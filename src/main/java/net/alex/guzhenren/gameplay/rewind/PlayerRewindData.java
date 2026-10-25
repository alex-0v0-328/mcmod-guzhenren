package net.alex.guzhenren.gameplay.rewind;

import com.mojang.serialization.Codec;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What the Man As Before Gu [人如故] remembers of its carrier: a ring of {@link PlayerSnapshot}s, oldest first,
 * following {@link RewindClock}. Saved on the player, never synced (the client never decides whether a day is
 * remembered), and not carried through a death: a new body starts with no past.
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
