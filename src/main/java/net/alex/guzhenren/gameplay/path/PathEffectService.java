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
 * 玩家流派效果（{@link PathEffectData}）的唯一运行时写入与状态栏图标投影服务。
 * <ul>
 *   <li>{@link #apply} 挂载新效果并顶替同槽旧效果（发布 {@link Reason#REPLACED}），{@link #update} 原位更新时长、层数或
 *       {@code payload}，{@link #remove} 按条件摘除（发布 {@link Reason#REMOVED}），{@link #sync} 在同类型效果已带相同
 *       {@code payload} 时保持不动、否则挂载或摘除；所有被移除或心跳到期的效果均在落盘后发布
 *       {@link PathEffectEndedEvent}。</li>
 *   <li>⚠ 不含任何道痕[Path Marks]的效果无法挂载，若 {@link #update} 将道痕扣空则按 {@link Reason#REMOVED} 摘除；流派
 *       效果的具体业务作用由各机制通过 {@link #find} 查询实现，本服务不直接执行效果逻辑，也不从道痕反推力量点。</li>
 *   <li>{@link #tick} 在每次心跳中清理已到期的限时效果并重新投影原版状态栏图标（每次写入时也会立即重投影）：永久效果投
 *       影为无限时长，{@code /effect clear} 最多只能让状态栏图标消失至多 1 秒，无法清除底层的流派效果。</li>
 *   <li>多个流派效果投影到同一个原版药水图标时，取最长剩余时间与最高等级；若玩家身上已有等级相同且剩余时长不短于目标的
 *       同种图标，则跳过重挂，避免稳定心跳期间每拍重发药水同步包。</li>
 * </ul>
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
