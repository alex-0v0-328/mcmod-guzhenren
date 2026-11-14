package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;
import net.alex.guzhenren.core.Ticks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 人如故[Man As Before Gu]记忆的节奏与快照环算术。
 * <ul>
 *   <li>每 {@link #INTERVAL_TICKS} 记一份（每天 2 次），环留 {@link #RING_SIZE} 份，恒跨一天；「一天前」= 至少
 *       {@link #MIN_AGE_TICKS}（整一天）前最新的一份，还原落在 1–1.5 天前；游戏时间即世界时钟，睡过的夜不算一天，宙道自
 *       身时间不缩放。</li>
 *   <li>⚠ 时间戳皆为游戏时间，{@link #NEVER} 表示无，{@code 0} 是真实时刻；超前于时钟的时间戳（时间倒退的世界）由
 *       {@link #add} 丢弃，环不提供未来。</li>
 * </ul>
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
