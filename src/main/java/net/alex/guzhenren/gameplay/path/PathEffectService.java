package net.alex.guzhenren.gameplay.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * 流派效果的唯一运行时写入方。
 *
 * <ul>
 *   <li>{@link #apply} 挂上并顶掉同格旧效果，{@link #update} 改（更久、多一层、新 payload）保留原格，{@link #remove}
 *       按条件摘除；以上或 {@link #tick} 丢掉的效果均在写存储后发 {@link PathEffectEndedEvent}。</li>
 *   <li>⚠ 无道痕[Path Marks]效果不入存储：挂不上，改成无道痕按 {@code REMOVED} 摘除；只有在与不在，不拿部分道痕。</li>
 *   <li>⚠ 作用由效果族代码读 {@link #find} 实现；本服务不运行效果，也不从道痕读力量。</li>
 *   <li>{@link #tick} 随心跳丢掉到期限时效果，再给其余重投影图标，{@code /effect clear} 至多让图标消失 1 秒、
 *       拿不走效果；永久效果投影无限时长；每次写入立即重投影。</li>
 *   <li>图标是每类效果投影的原版效果，多个投影同一个时取最长剩余与最高等级；玩家已戴着的（等级同、时长不短）不动：
 *       稳定心跳不加效果、不发添加事件、不发更新包。</li>
 * </ul>
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

    public static @NotNull PathMarks getMarks(@NotNull Player player, @NotNull GuPath path) {
        return get(player).getMarks(path);
    }

    public static <P> @Nullable PathEffect<P> find(@NotNull Player player, @NotNull PathEffectType<P> type) {
        return get(player).find(type, null);
    }

    public static <P> @Nullable PathEffect<P> find(@NotNull Player player, @NotNull PathEffectType<P> type,
                                                   @NotNull ResourceLocation source) {
        return get(player).find(type, source);
    }

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
        end(player, next.isEmpty() ? List.of(current) : List.of(), Reason.REMOVED);
        return true;
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

    /** 同类型效果已带相同 payload 时不动，否则挂上 {@code wanted}；{@code null} 摘掉该类型。 */
    public static <P> void sync(@NotNull ServerPlayer player, @NotNull PathEffectType<P> type,
                                @Nullable PathEffect<P> wanted) {
        PathEffect<P> worn = find(player, type);
        if (wanted == null) {
            if (worn != null) removeType(player, type);
        } else if (worn == null || !Objects.equals(worn.payload(), wanted.payload())) {
            apply(player, wanted);
        }
    }

    public static void tick(@NotNull ServerPlayer player) {
        long now = player.level().getGameTime();
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

    //region icons [图标]
    private static void refreshIcons(ServerPlayer player, Collection<PathEffect<?>> ended) {
        long now = player.level().getGameTime();
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
        wanted.forEach((icon, shown) -> {
            if (!isShown(player.getEffect(icon), shown)) {
                player.addEffect(ModEffects.instance(icon, shown[0], shown[1]));
            }
        });
    }

    private static boolean isShown(@Nullable MobEffectInstance worn, int[] shown) {
        return worn != null && worn.getAmplifier() == shown[1] && !worn.isAmbient() && !worn.isVisible()
                && worn.showIcon() && (worn.isInfiniteDuration()
                || shown[0] != MobEffectInstance.INFINITE_DURATION && worn.getDuration() >= shown[0]);
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
