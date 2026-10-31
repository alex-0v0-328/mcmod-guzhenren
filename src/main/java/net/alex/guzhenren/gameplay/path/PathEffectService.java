package net.alex.guzhenren.gameplay.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent.Reason;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The only runtime writer of path effects [流派效果]: put on, changed, cut, removed, run out, and projected as icons.
 *
 * <p>Static service over the {@code path_effect_data} attachment; reads take {@link Player}, writes take
 * {@link ServerPlayer}. {@link #apply} puts an effect on and drops whatever shared its slot; {@link #update} changes
 * one in place (a longer time, another layer, a new payload) and keeps its slot; {@link #cut} takes marks off one
 * path, each counter on its own; {@link #remove} takes effects off by a test. Every effect dropped, by any of these
 * or by {@link #tick}, posts a {@link PathEffectEndedEvent} after the store is written.
 *
 * <p>{@link #tick} runs on the heartbeat: it drops every timed effect whose end has passed, then re-projects the
 * icons of what is left, so {@code /effect clear} costs an icon for at most a second and never an effect.
 * A permanent effect projects an infinite icon. Each write re-projects at once, without waiting for the beat.
 *
 * <p>⚠ An effect with no marks is never stored: applying one does nothing, and cutting or changing one to none
 * removes it as {@code CUT}. ⚠ What an effect does is its family's code reading {@link #find}; this service runs no
 * effect and reads no strength from marks.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectData
 * @see PathEffect
 * @since 1.0.0
 */

public final class PathEffectService {

    private PathEffectService() {}

    public static @NotNull PathEffectData get(@NotNull Player player) { return PlayerData.get(player).getPathEffect(); }

    public static @NotNull List<PathEffect<?>> getEffects(@NotNull Player player) { return get(player).effects(); }

    public static @NotNull DaoMarks getMarks(@NotNull Player player, @NotNull GuPath path) {
        return get(player).getMarks(path);
    }

    public static <P> @Nullable PathEffect<P> find(@NotNull Player player, @NotNull PathEffectType<P> type) {
        return get(player).find(type, null);
    }

    public static <P> @Nullable PathEffect<P> find(@NotNull Player player, @NotNull PathEffectType<P> type,
                                                   @NotNull ResourceLocation source) {
        return get(player).find(type, source);
    }

    public static long getGameTime(@NotNull Player player) { return player.level().getGameTime(); }

    public static void apply(@NotNull ServerPlayer player, @NotNull PathEffect<?> effect) {
        if (effect.isEmpty()) return;

        PathEffectData data = get(player);
        List<PathEffect<?>> displaced = data.effects().stream().filter(effect::sharesSlot).toList();
        store(player, data.with(effect));
        end(player, displaced, Reason.REPLACED);
    }

    public static <P> boolean update(@NotNull ServerPlayer player, @NotNull PathEffectType<P> type,
                                     @NotNull ResourceLocation source, @NotNull UnaryOperator<PathEffect<P>> change) {
        PathEffect<P> current = find(player, type, source);
        if (current == null) return false;

        PathEffect<P> next = change.apply(current);
        List<PathEffect<?>> effects = new ArrayList<>(get(player).effects());
        effects.set(effects.indexOf(current), next);
        store(player, new PathEffectData(effects));
        end(player, next.isEmpty() ? List.of(current) : List.of(), Reason.CUT);
        return true;
    }

    public static <P> boolean cut(@NotNull ServerPlayer player, @NotNull PathEffectType<P> type,
                                  @NotNull ResourceLocation source, @NotNull GuPath path, @NotNull DaoMarks amount) {
        return update(player, type, source, effect -> effect.cut(path, amount));
    }

    public static int remove(@NotNull ServerPlayer player, @NotNull Predicate<PathEffect<?>> which) {
        PathEffectData data = get(player);
        List<PathEffect<?>> removed = data.effects().stream().filter(which).toList();
        if (removed.isEmpty()) return 0;

        store(player, data.without(which));
        end(player, removed, Reason.REMOVED);
        return removed.size();
    }

    public static int removeType(@NotNull ServerPlayer player, @NotNull PathEffectType<?> type) {
        return remove(player, effect -> effect.type() == type);
    }

    public static void tick(@NotNull ServerPlayer player) {
        long now = getGameTime(player);
        PathEffectData data = get(player);
        List<PathEffect<?>> expired = data.effects().stream().filter(effect -> effect.hasRunOut(now)).toList();
        if (!expired.isEmpty()) store(player, data.without(effect -> effect.hasRunOut(now)));
        end(player, expired, Reason.EXPIRED);
    }

    private static void store(ServerPlayer player, PathEffectData data) {
        PlayerData.get(player).setPathEffect(data);
    }

    private static void end(ServerPlayer player, List<PathEffect<?>> ended, Reason reason) {
        refreshIcons(player, ended);
        for (PathEffect<?> effect : ended) ModEvents.post(new PathEffectEndedEvent(player, effect, reason));
    }

    //region icons -- the vanilla effect each kind projects, longest remaining time and highest amplifier win
    private static void refreshIcons(ServerPlayer player, Collection<PathEffect<?>> ended) {
        long now = getGameTime(player);
        Map<Holder<MobEffect>, int[]> wanted = new LinkedHashMap<>();
        for (PathEffect<?> effect : get(player).effects()) {
            PathEffectType.Projection projection = effect.getProjection();
            if (projection == null) continue;

            int duration = getIconDuration(effect, now);
            wanted.merge(projection.effect(), new int[] { duration, projection.amplifier() },
                    (first, second) -> new int[] { getLonger(first[0], second[0]), Math.max(first[1], second[1]) });
        }
        for (PathEffect<?> effect : ended) {
            PathEffectType.Projection projection = effect.getProjection();
            if (projection != null && !wanted.containsKey(projection.effect())) {
                player.removeEffect(projection.effect());
            }
        }
        wanted.forEach((icon, shown) -> player.addEffect(ModEffects.instance(icon, shown[0], shown[1])));
    }

    private static int getIconDuration(PathEffect<?> effect, long now) {
        if (effect.isPermanent()) return MobEffectInstance.INFINITE_DURATION;
        return Math.clamp(effect.getTicksLeft(now), 1, Integer.MAX_VALUE);
    }

    private static int getLonger(int first, int second) {
        if (first == MobEffectInstance.INFINITE_DURATION || second == MobEffectInstance.INFINITE_DURATION) {
            return MobEffectInstance.INFINITE_DURATION;
        }
        return Math.max(first, second);
    }
    //endregion
}
