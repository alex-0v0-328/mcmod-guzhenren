package net.alex.guzhenren.core;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 收回丢弃物品的唯一安全方式，从不掉落。
 * <ul>
 *   <li>⚠ {@code placeItemBackInInventory} 掉落放不下的部分并再发 {@code ItemTossEvent}：取消丢弃后这样放回，背包满时递
 *       归至服务器崩溃；关界面时光标堆叠或 2x2 合成格回落即可触发。</li>
 *   <li>{@link #putBack}：放入能放下的，返回余下，不动丢出实体；余下另存的调用方总可取消丢弃。</li>
 *   <li>{@link #stow}：余下留在丢出实体上，无重力悬停原处，不落虚空，有空位可拾；返回是否全部放下，仅全放下才可取消丢
 *       弃，因事件已把实体移出世界，余下未放下却取消会删掉它。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
