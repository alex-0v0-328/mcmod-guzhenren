package net.alex.guzhenren.gameplay.mind;

import com.google.common.math.LongMath;
import java.util.Map;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only writer of Mind [脑海] pools, and the door where every clamp lives. Static service; {@code
 * setCurrent} is the single clamp -- it reads {@link WisdomType#isBurstable} to decide whether the value
 * may exceed the cap; {@code regenStep} is the heartbeat entry, a zombie thinking every 5th second.
 *
 * <p>⚠ The clamp cannot live in the record: only some wisdom types may burst, and a pool does not know
 * its type. ⚠ Regen always stops at the cap -- the body must never idle itself into 脑海炸裂 [mind
 * ocean shattered]; only a Gu, item or command may overfill. ⚠ {@code addThoughts} writes the tag total
 * AFTER the current -- call {@code setCurrent} first, or the tag map oversums.
 *
 * @author Alex
 * @version 1.0.0
 * @see MindData
 * @see MindPool
 * @since 1.0.0
 */

public final class MindService {

    private MindService() {}

    public static final int ZOMBIE_THOUGHT_INTERVAL_TICKS = 5 * Ticks.SECOND;

    private static boolean thinksThisStep(ServerPlayer p) {
        return !BodyService.isZombie(p) || p.tickCount % ZOMBIE_THOUGHT_INTERVAL_TICKS == 0;
    }

    public static @NotNull MindData get(@NotNull Player p) { return p.getData(ModAttachments.MIND); }

    public static @NotNull MindPool pool(@NotNull Player p, @NotNull WisdomType t) { return get(p).pool(t); }

    public static long current(@NotNull Player p, @NotNull WisdomType t) { return pool(p, t).current(); }

    public static long max(@NotNull Player p, @NotNull WisdomType t) { return pool(p, t).max(); }

    public static @NotNull Brilliance brilliance(@NotNull Player p) { return get(p).brilliance(); }

    public static @NotNull Map<ThoughtTag, Long> taggedThoughts(@NotNull Player p) { return get(p).taggedThoughts(); }

    public static long taggedAmount(@NotNull Player p, @NotNull ThoughtTag tag) {
        return get(p).taggedThoughts().getOrDefault(tag, 0L);
    }

    public static long naturalThoughts(@NotNull Player p) {
        long tagged = 0L;
        for (long amount : get(p).taggedThoughts().values()) tagged = LongMath.saturatedAdd(tagged, amount);
        return Math.max(0L, current(p, WisdomType.THOUGHTS) - tagged);
    }

    public static void setCurrent(@NotNull ServerPlayer p, @NotNull WisdomType t, long v) {
        MindPool pool = pool(p, t);
        set(p, t, pool.withCurrent(t.isBurstable() ? v : Math.min(v, pool.max())));
    }

    public static void addCurrent(@NotNull ServerPlayer p, @NotNull WisdomType t, long d) {
        setCurrent(p, t, LongMath.saturatedAdd(current(p, t), d));
    }

    public static void addThoughts(@NotNull ServerPlayer p, long amount, @NotNull ThoughtTag tag) {
        if (amount <= 0L) return;
        setCurrent(p, WisdomType.THOUGHTS, LongMath.saturatedAdd(current(p, WisdomType.THOUGHTS), amount));
        if (tag != ThoughtTag.NATURAL) {
            store(p, get(p).withTagged(tag, LongMath.saturatedAdd(taggedAmount(p, tag), amount)));
        }
    }

    public static void setMax(@NotNull ServerPlayer p, @NotNull WisdomType t, long v) {
        set(p, t, pool(p, t).withMax(v));
    }

    public static void addMax(@NotNull ServerPlayer p, @NotNull WisdomType t, long d) {
        setMax(p, t, LongMath.saturatedAdd(max(p, t), d));
    }

    public static void empty(@NotNull ServerPlayer p) { store(p, get(p).emptied()); }

    private static void set(ServerPlayer p, WisdomType t, MindPool v) { store(p, get(p).with(t, v)); }

    private static void store(ServerPlayer p, MindData d) { p.setData(ModAttachments.MIND, d); }

    public static void setBrilliance(@NotNull ServerPlayer p, @NotNull Brilliance v) {
        store(p, get(p).withBrilliance(v));
    }

    public static void shiftBrilliance(@NotNull ServerPlayer p, int d) { setBrilliance(p, brilliance(p).shift(d)); }

    public static void refill(@NotNull ServerPlayer p, @NotNull WisdomType t) {
        long cap = max(p, t);
        set(p, t, new MindPool(cap, cap, false));
    }

    public static void regenStep(@NotNull ServerPlayer player) {
        if (!thinksThisStep(player)) return;

        MindPool thoughts = pool(player, WisdomType.THOUGHTS);
        if (thoughts.current() >= thoughts.max()) return;

        long grown = LongMath.saturatedAdd(thoughts.current(),
                PathTimeFlowService.perStep(player, brilliance(player).getThoughtsPerSecond()));
        setCurrent(player, WisdomType.THOUGHTS, Math.min(grown, thoughts.max()));
    }

    public static void onSleepComplete(@NotNull ServerPlayer p) {
        set(p, WisdomType.THOUGHTS, pool(p, WisdomType.THOUGHTS).slept());
    }
}
