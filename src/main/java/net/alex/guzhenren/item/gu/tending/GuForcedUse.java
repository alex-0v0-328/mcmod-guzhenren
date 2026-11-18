package net.alex.guzhenren.item.gu.tending;

import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 强行催动[Forced Use]二次确认窗口管理器：由 {@code TendedGuItem.drive} 调用 {@link #confirm} 判定是否允许放行。
 * <ul>
 *   <li>自动进食后仍付不起饱食[Hunger]代价的催动会先被拒绝并弹出红字警告；玩家在 {@link #WINDOW_TICKS} 内再次催动同一只
 *       蛊才会放行，并在效果生效后使蛊虫力竭而死。</li>
 *   <li>⚠ 确认窗口仅保存在服务端内存中且属于临时状态，每位玩家至多记录一条，不写入存档或物品栈组件，随玩家死亡或登出而
 *       丢失。</li>
 *   <li>「同一只」按物品栈对象引用比对，同种类的两只蛊互不替代；手动整理背包可能产生新物品栈实例，最坏代价也只是重新弹
 *       出警告，绝不会误杀玩家手中其他蛊虫。</li>
 * </ul>
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
