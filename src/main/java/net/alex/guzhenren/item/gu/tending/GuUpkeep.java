package net.alex.guzhenren.item.gu.tending;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 日切时对已炼化持续照顾型蛊[Tended Gu]的遍历。
 * <ul>
 *   <li>三种容器、两种时钟皆经此结算：{@link #tickInContainer} 管空窍[Aperture]中的蛊，{@link #tickCarried}
 *       管随身蛊；每只先付维持开销，再由饱食[Hunger]时钟判饿死，随身饿死者移除并交 {@code TendedGuItem.starve}。</li>
 *   <li>⚠ 顺序为进食、扣量、再进食、警告，看似随意，但交换任两步都会改变哪些蛊活过一天。</li>
 *   <li>首次进食救下先前背包空而挨饿的蛊（存放的蛊不能催动，扣量前别无喂法）；两次进食都从背包、仅在跨日心跳
 *       （{@code days > 0}），每次先修复，食物全耗于伤势者仍被警告。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuHunger
 * @since 1.0.0
 */

public final class GuUpkeep {

    private GuUpkeep() {}

    public static void tickCarried(ServerPlayer player, long days) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (tickInContainer(player, stack, days)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                TendedGuItem.starve(player, stack);
            }
        }
    }

    public static boolean tickInContainer(ServerPlayer player, ItemStack stack, long days) {
        if (!(stack.getItem() instanceof TendedGuItem gu) || !gu.isRefined(stack)) return false;

        gu.payOwnUpkeep(player, stack);
        if (days > 0L) gu.feed(player, stack);
        if (gu.hunger.tickStarvation(player, stack, days)) return true;

        // Runs every heartbeat while the bar drains only on a rollover: warning on any other beat would repeat it.
        if (days > 0L) gu.feedAndWarn(player, stack);
        return false;
    }
}
