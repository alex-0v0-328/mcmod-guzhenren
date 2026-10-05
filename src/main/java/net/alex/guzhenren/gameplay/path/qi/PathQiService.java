package net.alex.guzhenren.gameplay.path.qi;

import com.google.common.math.LongMath;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only runtime writer of Qi [气] holdings, and where their MobEffects are rebuilt from the pool. Static
 * service; every {@code store} also runs {@code syncEffects}, rebuilding the four qi MobEffects.
 *
 * <p>⚠ Those effects are a PROJECTION, never the truth -- the heartbeat rebuilds them, so milk and
 * {@code /effect clear} cannot cure Death Qi [死气]. ⚠ {@code set} re-anchors the hold on the SUM,
 * never "takes the higher grade" and never refuses -- death qi accumulates like every other kind. ⚠
 * The graded syncs compute the tier off the CURRENT amount only while the kind is holding; an expired
 * kind has no projected effect.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathQiData
 * @see PathQiEntry
 * @since 1.0.0
 */

public final class PathQiService {

    private PathQiService() {}

    private static final int EFFECT_REFRESH_TICKS = 2 * Ticks.SECOND;
    private static final int DEATH_QI_EFFECT_TICKS = 10 * EFFECT_REFRESH_TICKS;

    public static @NotNull PathQiData get(@NotNull Player player) { return player.getData(ModAttachments.PATH_QI); }

    public static long getCurrent(@NotNull Player player, @NotNull QiKind kind) {
        return get(player).current(kind, now(player));
    }

    public static void add(@NotNull ServerPlayer player, @NotNull QiKind kind, long delta) {
        set(player, kind, LongMath.saturatedAdd(getCurrent(player, kind), delta));
    }

    public static void set(@NotNull ServerPlayer player, @NotNull QiKind kind, long value) {
        long amount = Math.max(0L, value);
        long holdEnd = 0L;
        if (kind.isTimed() && amount > 0L) {
            holdEnd = now(player) + kind.holdTicks(Math.max(0, QiKind.tierOf(amount)));
        }
        store(player, get(player).with(kind, new PathQiEntry(amount, holdEnd)));
    }

    private static long now(Player player) { return player.level().getGameTime(); }

    private static void store(ServerPlayer player, PathQiData data) {
        long now = now(player);
        PathQiData pruned = data;
        for (QiKind kind : QiKind.values()) {
            if (data.get(kind) != null && data.current(kind, now) <= 0L) pruned = pruned.without(kind);
        }
        if (pruned.equals(get(player))) return;
        player.setData(ModAttachments.PATH_QI, pruned);
        syncEffects(player);
    }

    //region effect projection -- the store is the truth, the MobEffect is its display
    public static void syncEffects(@NotNull ServerPlayer player) {
        long now = now(player);
        syncGraded(player, QiKind.STRENGTH, ModEffects.STRENGTH_QI, now);
        syncGraded(player, QiKind.LIFE, ModEffects.LIFE_QI, now);
        syncGraded(player, QiKind.ESSENCE, ModEffects.ESSENCE_QI, now);
        syncDeath(player, now);
    }

    private static void syncGraded(ServerPlayer player, QiKind kind, Holder<MobEffect> effect, long now) {
        PathQiData data = get(player);
        int tier = data.holding(kind, now) ? QiKind.tierOf(data.current(kind, now)) : -1;
        MobEffectInstance current = player.getEffect(effect);

        if (tier < 0) {
            if (current != null) player.removeEffect(effect);
            return;
        }

        PathQiEntry entry = data.get(kind);
        int duration = entry == null ? EFFECT_REFRESH_TICKS
                : (int) Math.min(Integer.MAX_VALUE, entry.holdEndTick() - now + EFFECT_REFRESH_TICKS);
        if (current == null || current.getAmplifier() != tier
                || current.getDuration() < EFFECT_REFRESH_TICKS) {
            player.addEffect(ModEffects.instance(effect, duration, tier));
        }
    }

    private static void syncDeath(ServerPlayer player, long now) {
        boolean present = get(player).current(QiKind.DEATH, now) > 0L;
        MobEffectInstance current = player.getEffect(ModEffects.DEATH_QI);
        if (!present) {
            if (current != null) player.removeEffect(ModEffects.DEATH_QI);
            return;
        }
        if (current == null || current.getDuration() < EFFECT_REFRESH_TICKS) {
            player.addEffect(ModEffects.instance(ModEffects.DEATH_QI, DEATH_QI_EFFECT_TICKS, 0));
        }
    }
    //endregion
}
