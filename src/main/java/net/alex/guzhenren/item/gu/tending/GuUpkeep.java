package net.alex.guzhenren.item.gu.tending;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 每秒心跳对已炼化持续照顾型蛊[Tended Gu]的维持遍历工具。
 * <ul>
 *   <li>遍历覆盖三类存储位置（随身背包经 {@link #tickCarried} 由 {@code Heartbeat.beat} 直调，各空窍存储格与本命槽经
 *       {@link #tickInContainer} 由 {@code ApertureStorageTick} 调用）与两类时钟（饱食[Hunger]条按游戏日扣减，回复条按
 *       世界时钟结算）；每只蛊先支付维持开销再结算饿死，随身饿死者从背包移除并交由 {@code TendedGuItem.starve} 公
 *       告。</li>
 *   <li>⚠ 内部执行顺序固定为「进食、扣量、再进食、警告」，交换任意两步都会改变饥荒临界状态下的存活结果。</li>
 *   <li>扣量前先执行一次进食，可救下此前因背包无食而处于饥饿边缘的蛊虫（存放在空窍中的蛊无法催动，扣量前别无喂食途
 *       径）；两次进食均从背包取食，且扣量后仅在跨日心跳（{@code days > 0}）警告以防每秒心跳重复提示；每次进食都优先
 *       修复伤势，若食物全数耗于修伤而饱食仍低于阈值则仍会发出饥饿警告。</li>
 * </ul>
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

        if (days > 0L) gu.feedAndWarn(player, stack);
        return false;
    }
}
