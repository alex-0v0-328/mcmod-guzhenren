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
 * The only runtime writer of Mind [脑海] pools, and the door where every clamp lives. Static service; {@code
 * setCurrent} is the single clamp -- it reads {@link MindPoolType#isBurstable} to decide whether the value
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

    private static boolean thinksThisStep(ServerPlayer player) {
        return !BodyService.isZombie(player) || player.tickCount % ZOMBIE_THOUGHT_INTERVAL_TICKS == 0;
    }

    public static @NotNull MindData get(@NotNull Player player) { return player.getData(ModAttachments.MIND); }

    public static @NotNull MindPool getPool(@NotNull Player player, @NotNull MindPoolType type) {
        return get(player).pool(type);
    }

    public static long getCurrent(@NotNull Player player, @NotNull MindPoolType type) {
        return getPool(player, type).current();
    }

    public static long getMax(@NotNull Player player, @NotNull MindPoolType type) {
        return getPool(player, type).max();
    }

    public static @NotNull Brilliance getBrilliance(@NotNull Player player) { return get(player).brilliance(); }

    public static @NotNull Map<ThoughtTag, Long> getTaggedThoughts(@NotNull Player player) {
        return get(player).taggedThoughts();
    }

    public static long getTaggedAmount(@NotNull Player player, @NotNull ThoughtTag tag) {
        return get(player).taggedThoughts().getOrDefault(tag, 0L);
    }

    public static long getNaturalThoughts(@NotNull Player player) {
        long tagged = 0L;
        for (long amount : get(player).taggedThoughts().values()) tagged = LongMath.saturatedAdd(tagged, amount);
        return Math.max(0L, getCurrent(player, MindPoolType.THOUGHTS) - tagged);
    }

    public static void setCurrent(@NotNull ServerPlayer player, @NotNull MindPoolType type, long value) {
        MindPool pool = getPool(player, type);
        set(player, type, pool.withCurrent(type.isBurstable() ? value : Math.min(value, pool.max())));
    }

    public static void addCurrent(@NotNull ServerPlayer player, @NotNull MindPoolType type, long delta) {
        setCurrent(player, type, LongMath.saturatedAdd(getCurrent(player, type), delta));
    }

    public static void addThoughts(@NotNull ServerPlayer player, long amount, @NotNull ThoughtTag tag) {
        if (amount <= 0L) return;
        setCurrent(player, MindPoolType.THOUGHTS,
                LongMath.saturatedAdd(getCurrent(player, MindPoolType.THOUGHTS), amount));
        if (tag != ThoughtTag.NATURAL) {
            store(player, get(player).withTagged(tag, LongMath.saturatedAdd(getTaggedAmount(player, tag), amount)));
        }
    }

    public static void setMax(@NotNull ServerPlayer player, @NotNull MindPoolType type, long value) {
        set(player, type, getPool(player, type).withMax(value));
    }

    public static void addMax(@NotNull ServerPlayer player, @NotNull MindPoolType type, long delta) {
        setMax(player, type, LongMath.saturatedAdd(getMax(player, type), delta));
    }

    public static void empty(@NotNull ServerPlayer player) { store(player, get(player).emptied()); }

    private static void set(ServerPlayer player, MindPoolType type, MindPool pool) {
        store(player, get(player).with(type, pool));
    }

    private static void store(ServerPlayer player, MindData data) { player.setData(ModAttachments.MIND, data); }

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

    public static void regenStep(@NotNull ServerPlayer player) {
        if (!thinksThisStep(player)) return;

        MindPool thoughts = getPool(player, MindPoolType.THOUGHTS);
        if (thoughts.current() >= thoughts.max()) return;

        long grown = LongMath.saturatedAdd(thoughts.current(),
                PathTimeFlowService.scale(player, getBrilliance(player).getThoughtsPerSecond()));
        setCurrent(player, MindPoolType.THOUGHTS, Math.min(grown, thoughts.max()));
    }

    public static void onSleepComplete(@NotNull ServerPlayer player) {
        set(player, MindPoolType.THOUGHTS, getPool(player, MindPoolType.THOUGHTS).slept());
    }
}
