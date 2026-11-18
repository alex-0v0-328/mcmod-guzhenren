package net.alex.guzhenren.core;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 拦截丢弃事件并安全收回物品的工具类，保证剩余物品绝不触发二次掉落循环。
 * <ul>
 *   <li>{@link #putBack} 尝试将丢出实体的物品塞回玩家背包并返回未能放入的剩余堆叠（不修改原掉落物实体），供会将剩余物品
 *       另行存储的调用方无条件取消丢弃事件。</li>
 *   <li>{@link #stow} 在背包塞满剩余堆叠时将其写回掉落物实体，并设为无重力、零速度原处悬停以防掉入虚空，待玩家腾出空位
 *       后自动拾取；仅当全部物品均塞入背包时返回 {@code true}。</li>
 *   <li>⚠ 严禁在 {@code ItemTossEvent} 中调用原版 {@code Inventory#placeItemBackInInventory}：该方法在背包已满时会将放
 *       不下的部分重新丢出并再次触发 {@code ItemTossEvent}，导致关闭界面归还光标物品或合成格物品时陷入无限递归直至服务
 *       端崩溃。</li>
 *   <li>⚠ 调用 {@link #stow} 的事件处理器仅可在其返回 {@code true}（全部塞回背包）时取消 {@code ItemTossEvent}；因为事
 *       件触发时掉落物实体尚未加入世界，若在存在剩余物品时取消事件会导致剩余物品直接消失。</li>
 * </ul>
 */

public final class TossedItems {

    private TossedItems() {}

    public static @NotNull ItemStack putBack(@NotNull Player player, @NotNull ItemEntity tossed) {
        ItemStack rest = tossed.getItem().copy();
        player.getInventory().add(rest);
        return rest;
    }

    public static boolean stow(@NotNull Player player, @NotNull ItemEntity tossed) {
        ItemStack rest = putBack(player, tossed);
        if (rest.isEmpty()) return true;

        tossed.setItem(rest);
        tossed.setNoGravity(true);
        tossed.setDeltaMovement(Vec3.ZERO);
        return false;
    }
}
