package net.alex.guzhenren.gameplay.mind;

import com.google.common.math.LongMath;
import java.util.Map;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 脑海三池与才情[Brilliance]的唯一运行时写入服务。
 * <ul>
 *   <li>⚠ 池当前值的上限夹取仅在 {@link #setCurrent}（底层 {@code withCurrent}）中按 {@link MindPoolType#isBurstable}
 *       执行，不可放入 {@link MindPool} 构造器：因为仅部分池允许超限写入，且 {@link MindPool} 本身不持有池类型。</li>
 *   <li>{@link #regenStep} 为每秒心跳自然回念入口：永久僵尸（{@code BodyService.isZombie}）每 5 秒（
 *       {@link #ZOMBIE_THOUGHT_INTERVAL_TICKS}）才自然回复一次念头，临时僵尸化照常每秒回复。</li>
 *   <li>⚠ 自然回复严格封顶于 {@code max}，挂机永远不会撑爆脑海；唯有蛊虫、物品或管理员指令可向允许超限的池写入超出上限
 *       的数值。</li>
 *   <li>⚠ {@link #addThoughts} 必须先调用 {@code withCurrent} 增加念池总量，再写入 {@code taggedThoughts} 标签份额，否
 *       则新标签份额会因超出旧总量而被 {@link MindData} 紧凑构造器按比例截断。</li>
 * </ul>
 */

public final class MindService {

    private MindService() {}

    public static final int ZOMBIE_THOUGHT_INTERVAL_TICKS = 5 * Ticks.SECOND;

    private static boolean canThinkThisStep(ServerPlayer player) {
        return !BodyService.isZombie(player) || player.tickCount % ZOMBIE_THOUGHT_INTERVAL_TICKS == 0;
    }

    public static @NotNull MindData get(@NotNull Player player) { return PlayerData.get(player).getMind(); }

    public static @NotNull MindPool getPool(@NotNull Player player, @NotNull MindPoolType type) {
        return get(player).getPool(type);
    }

    public static long getCurrent(@NotNull Player player, @NotNull MindPoolType type) {
        return getPool(player, type).current();
    }

    public static long getMax(@NotNull Player player, @NotNull MindPoolType type) {
        return getPool(player, type).max();
    }

    private static @NotNull Brilliance getBrilliance(@NotNull Player player) { return get(player).brilliance(); }

    public static @NotNull Map<ThoughtTag, Long> getTaggedThoughts(@NotNull Player player) {
        return get(player).taggedThoughts();
    }

    public static long getNaturalThoughts(@NotNull Player player) {
        long tagged = 0L;
        for (long amount : get(player).taggedThoughts().values()) tagged = LongMath.saturatedAdd(tagged, amount);
        return Math.max(0L, getCurrent(player, MindPoolType.THOUGHTS) - tagged);
    }

    public static void setCurrent(@NotNull ServerPlayer player, @NotNull MindPoolType type, long value) {
        store(player, withCurrent(get(player), type, value));
    }

    public static void addCurrent(@NotNull ServerPlayer player, @NotNull MindPoolType type, long delta) {
        setCurrent(player, type, LongMath.saturatedAdd(getCurrent(player, type), delta));
    }

    public static void addThoughts(@NotNull ServerPlayer player, long amount, @NotNull ThoughtTag tag) {
        if (amount <= 0L) return;
        MindData data = get(player);
        MindData added = withCurrent(data, MindPoolType.THOUGHTS,
                LongMath.saturatedAdd(data.getPool(MindPoolType.THOUGHTS).current(), amount));
        if (tag != ThoughtTag.NATURAL) {
            long tagged = added.taggedThoughts().getOrDefault(tag, 0L);
            added = added.withTagged(tag, LongMath.saturatedAdd(tagged, amount));
        }
        store(player, added);
    }

    public static void setMax(@NotNull ServerPlayer player, @NotNull MindPoolType type, long value) {
        set(player, type, getPool(player, type).withMax(value));
    }

    public static void addMax(@NotNull ServerPlayer player, @NotNull MindPoolType type, long delta) {
        setMax(player, type, LongMath.saturatedAdd(getMax(player, type), delta));
    }

    public static void empty(@NotNull ServerPlayer player) { store(player, get(player).withEmptied()); }

    public static void restoreThoughts(@NotNull ServerPlayer player, @NotNull Map<MindPoolType, Long> current,
                                       @NotNull Map<ThoughtTag, Long> tagged) {
        MindData data = get(player);
        for (Map.Entry<MindPoolType, Long> entry : current.entrySet()) {
            data = withCurrent(data, entry.getKey(), entry.getValue());
        }
        store(player, new MindData(data.brilliance(), data.pools(), tagged));
    }

    private static MindData withCurrent(MindData data, MindPoolType type, long value) {
        MindPool pool = data.getPool(type);
        return data.with(type, pool.withCurrent(type.isBurstable() ? value : Math.min(value, pool.max())));
    }

    private static void set(ServerPlayer player, MindPoolType type, MindPool pool) {
        store(player, get(player).with(type, pool));
    }

    private static void store(ServerPlayer player, MindData data) {
        PlayerData.get(player).setMind(data);
    }

    public static void setBrilliance(@NotNull ServerPlayer player, @NotNull Brilliance brilliance) {
        store(player, get(player).withBrilliance(brilliance));
    }

    public static void shiftBrilliance(@NotNull ServerPlayer player, int delta) {
        setBrilliance(player, getBrilliance(player).shift(delta));
    }

    public static void refill(@NotNull ServerPlayer player, @NotNull MindPoolType type) {
        long cap = getMax(player, type);
        set(player, type, new MindPool(cap, cap, false));
    }

    public static void regenStep(@NotNull ServerPlayer player, @NotNull Beat beat) {
        if (!canThinkThisStep(player)) return;

        MindPool thoughts = getPool(player, MindPoolType.THOUGHTS);
        if (thoughts.current() >= thoughts.max()) return;

        long gained = beat.scale(getBrilliance(player).getThoughtsPerSecond());
        long grown = LongMath.saturatedAdd(thoughts.current(), gained);
        setCurrent(player, MindPoolType.THOUGHTS, Math.min(grown, thoughts.max()));
    }

    public static void onSleepComplete(@NotNull ServerPlayer player) {
        set(player, MindPoolType.THOUGHTS, getPool(player, MindPoolType.THOUGHTS).withSlept());
    }
}
