package net.alex.guzhenren.item.gu.stack;

import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.jetbrains.annotations.Nullable;

/**
 * 持续照顾型蛊[Tended Gu]（{@link TendedGuItem}）的物品栈长冷却时间戳工具：原版物品冷却仅用于绘制扇形转圈动画，物品栈上
 * 的主世界游戏时间戳才是跨重登与跨容器的冷却真值。
 * <ul>
 *   <li>⚠ {@link #createCooldownStamp} 在写入时间戳时直接将玩家当前时间流速（{@link TimeFlow}）缩短的等待差额往回扣
 *       减，避免在读取剩余冷却时二次缩放导致已结束或进行中的冷却因流速变化而跳变。</li>
 *   <li>⚠ 减速时间流速会使写入的时间戳超前于当前主世界游戏时间：{@link #stampCooldownLeft} 允许在最慢流速
 *       （{@link TimeFlow#MIN_RATE}）拉长的最大窗口范围内识别超前时间戳；若超前量越过该上限，则判定为跨存档迁移的异世界
 *       时间戳并直接视为无冷却。</li>
 * </ul>
 */

public final class GuCooldowns {

    private GuCooldowns() {}

    public static long createCooldownStamp(ServerPlayer player, int window) {
        long gameTime = player.server.overworld().getGameTime();
        return gameTime - (window - TimeFlow.shortenWait(player, window));
    }

    public static int stampCooldownLeft(long now, @Nullable Long stamp, int window) {
        if (stamp == null || window <= 0) return 0;

        long left = window - (now - stamp);
        return left <= 0L || left > TimeFlow.shortenWait(TimeFlow.MIN_RATE, window) ? 0 : (int) left;
    }

    public static int getCooldownLeft(Player player, @Nullable Long stamp, int window) {
        MinecraftServer server = player.getServer();
        return server == null ? 0 : stampCooldownLeft(server.overworld().getGameTime(), stamp, window);
    }

    public static void applyPostRefineCooldown(ItemCooldowns cooldowns, Item item, int wanted) {
        if (!cooldowns.isOnCooldown(item)) cooldowns.addCooldown(item, wanted);
    }
}
