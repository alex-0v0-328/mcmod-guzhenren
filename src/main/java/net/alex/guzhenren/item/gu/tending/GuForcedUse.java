package net.alex.guzhenren.item.gu.tending;

import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 强行催动[Forced Use]确认窗口；{@code TendedGuItem.drive} 调 {@link #confirm}。
 * <ul>
 *   <li>进食后付不起饱食[Hunger]的催动先拒并警告；{@link #WINDOW_TICKS} 内再催同一只才放行，效果落地后蛊力竭而死。</li>
 *   <li>⚠ 窗口仅服务端、临时：每玩家一个，不进存档或组件，随玩家对象消失（死亡、登出）。</li>
 *   <li>「同一只」指同一物品堆对象，同种两只不互认；手动挪动可能变新对象，代价是重新警告，绝不误杀。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuHunger
 * @since 1.0.0
 */

public final class GuForcedUse {

    public static final int WINDOW_TICKS = 5 * Ticks.SECOND;
    private static final Map<ServerPlayer, Pending> PENDING = new WeakHashMap<>();

    private record Pending(ItemStack stack, int until) {}

    private GuForcedUse() {}

    /** 窗口内已警告过该物品则返回 true 并关窗；否则重新警告。 */
    public static boolean confirm(ServerPlayer player, ItemStack stack) {
        int now = player.server.getTickCount();
        Pending pending = PENDING.remove(player);
        if (pending != null && pending.stack() == stack && now <= pending.until()) return true;

        PENDING.put(player, new Pending(stack, now + WINDOW_TICKS));
        return false;
    }
}
