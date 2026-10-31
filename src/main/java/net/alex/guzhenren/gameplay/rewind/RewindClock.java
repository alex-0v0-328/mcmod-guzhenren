package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;
import net.alex.guzhenren.core.Ticks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The cadence the Man As Before Gu [人如故] memory keeps, and the arithmetic over a ring of stamped snapshots: when the
 * next snapshot is due, which one is "a day ago", and what a ring keeps after a new one lands.
 *
 * <p>A snapshot is taken twice a day, every {@link #INTERVAL_TICKS} of game time, and the ring keeps
 * {@link #RING_SIZE} of them, so it always spans one day. "A day ago" is the youngest snapshot at least
 * {@link #MIN_AGE_TICKS} -- a full day -- old, so a restore lands between one day and a day and a half back and never
 * on something younger. Game time is the world clock: a slept-through night is not a day gone, and the Time Path
 * [宙道] self-time never scales it.
 *
 * <p>⚠ Every stamp is game time, and {@link #NEVER} means no stamp: {@code 0} is a real game moment. A stamp ahead
 * of the clock (a world whose time ran backwards) is dropped by {@link #add}, so the ring never serves the future.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerRewindService
 * @since 1.0.0
 */

public final class RewindClock {

    private RewindClock() {}

    public static final int DAY_TICKS = Ticks.DAY;
    public static final int INTERVAL_TICKS = Ticks.HALF_DAY;
    public static final int RING_SIZE = DAY_TICKS / INTERVAL_TICKS + 1;
    public static final int MIN_AGE_TICKS = DAY_TICKS;
    public static final long NEVER = -1L;

    public static boolean isDue(long lastTakenAt, long now) {
        return lastTakenAt == NEVER || now < lastTakenAt || now - lastTakenAt >= INTERVAL_TICKS;
    }

    public static <T> long lastTakenAt(@NotNull List<T> ring, @NotNull ToLongFunction<T> stamp) {
        return ring.isEmpty() ? NEVER : stamp.applyAsLong(ring.getLast());
    }

    public static <T> @Nullable T pick(@NotNull List<T> ring, @NotNull ToLongFunction<T> stamp, long now) {
        for (int i = ring.size() - 1; i >= 0; i--) {
            if (now - stamp.applyAsLong(ring.get(i)) >= MIN_AGE_TICKS) return ring.get(i);
        }
        return null;
    }

    public static <T> @NotNull List<T> add(@NotNull List<T> ring, @NotNull T next, @NotNull ToLongFunction<T> stamp) {
        long now = stamp.applyAsLong(next);
        List<T> kept = new ArrayList<>(ring.size() + 1);
        for (T snapshot : ring) {
            if (stamp.applyAsLong(snapshot) <= now) kept.add(snapshot);
        }
        kept.add(next);
        while (kept.size() > RING_SIZE) kept.removeFirst();
        return List.copyOf(kept);
    }
}
