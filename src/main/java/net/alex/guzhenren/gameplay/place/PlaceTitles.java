package net.alex.guzhenren.gameplay.place;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

/**
 * 地名标题[Place Title]的服务端入口：任何特殊地域（天地秘境、以后的特殊地形）都经这里给玩家弹出屏幕标题。
 * <ul>
 *   <li>{@link #show} 无条件弹一次；{@link #update} 由调用方每次判定后报告玩家是否在某地点内，玩家进入且距上次身在该地点
 *       超过 {@link #COOLDOWN_TICKS} 刻（或从未到过）时才弹，所以沿边界来回走、或在里面停留都不会重复刷。</li>
 *   <li>地点用调用方自选的 {@link ResourceLocation} 区分（秘境用其结构集 ID），各地点冷却互不影响。</li>
 *   <li>时钟用玩家 {@code tickCount}；记录只在内存、按玩家实体弱引用，重进存档或重生得到新实体，会再弹一次。</li>
 *   <li>⚠ 调用间隔须远小于冷却：{@link #update} 在玩家停留期间持续刷新「上次在内」，间隔超过冷却会被当成重新进入。</li>
 *   <li>本类只发布 {@link PlaceTitleEvent}，不碰网络；下发与绘制在网络层与客户端。</li>
 * </ul>
 */

public final class PlaceTitles {

    private PlaceTitles() {}

    public static final int COOLDOWN_TICKS = 60 * Ticks.SECOND;
    private static final Map<ServerPlayer, Map<ResourceLocation, Integer>> LAST_INSIDE = new WeakHashMap<>();

    public static void show(@NotNull ServerPlayer player, @NotNull PlaceTitle title) {
        NeoForge.EVENT_BUS.post(new PlaceTitleEvent(player, title));
    }

    /** 报告玩家此刻是否在 {@code place} 内；返回这次是否弹出了标题。 */
    public static boolean update(@NotNull ServerPlayer player, @NotNull ResourceLocation place, boolean inside,
                                 @NotNull PlaceTitle title) {
        if (!inside) return false;

        Map<ResourceLocation, Integer> places = LAST_INSIDE.computeIfAbsent(player, ignored -> new HashMap<>());
        Integer last = places.put(place, player.tickCount);
        if (last != null && player.tickCount - last <= COOLDOWN_TICKS) return false;

        show(player, title);
        return true;
    }
}
