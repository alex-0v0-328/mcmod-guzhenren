package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;
import net.alex.guzhenren.core.Ticks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 人如故[Man As Before Gu]身体快照的采样间隔与环形队列算术纯函数。
 * <ul>
 *   <li>每隔 {@link #INTERVAL_TICKS}（半个游戏日，12,000 tick）采样一次，环形队列保留最近 {@link #RING_SIZE}（3）份快照
 *       以覆盖整个游戏日；{@link #pick} 选取距今至少 {@link #MIN_AGE_TICKS}（1 个游戏日）中最新的一份快照（还原点落在
 *       1–1.5 游戏日前）；计时采用世界真实 {@code gameTime}，睡眠跳夜不加速，也不受宙道自身时间倍率缩放。</li>
 *   <li>⚠ 快照时间戳未记录时统一用 {@link #NEVER}（{@code -1}）表示（{@code 0} 是新世界合法时刻）；当世界时间倒退导致已
 *       有快照时间戳超前于当前时间时，{@link #add} 会自动丢弃这些「未来」快照。</li>
 * </ul>
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
