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
 * 持续照顾型蛊[Tended Gu]的物品长冷却：原版只画转圈，物品时间戳（主世界游戏时间）为真值。
 * <ul>
 *   <li>⚠ {@link #createCooldownStamp} 按加速时钟已走份额往回盖：结束很久后仍会读，出口再缩放会重算进行中的冷却。</li>
 *   <li>⚠ 减速时钟往前盖：{@link #stampCooldownLeft} 认超前戳，上限为最慢时钟（{@code TimeFlow.MIN_RATE}）拉长的窗口；
 *       超出者来自别的世界，按无冷却。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
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
