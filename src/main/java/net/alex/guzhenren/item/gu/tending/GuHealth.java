package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 持续照顾型蛊[Tended Gu]的蛊虫生命：{@link RefinedGuState} 存已受伤害，未动过的野生蛊读作满血。
 * <ul>
 *   <li>{@link GuProperties#buildHealth} 取 4 × 转数 + 4（8–24），不用玩家的 {@code Rank.getMaxHealth}；归零即损毁
 *       （{@code TendedGuItem.ruin}），本命蛊例外，0 生命仍占槽。</li>
 *   <li>食物按饱食[Hunger]修复，1 生命 = 1 饱食（{@code unitsPerHunger}）；{@link #mendFrom} 只吃整件，不超伤势。</li>
 *   <li>⚠ 生命写入都经宿主 {@code store}（钳 {@code damageTaken}），唯 {@code RecoverBar.settleHealth} 只缩已钳伤势；
 *       饱食条在 {@code GuHunger.storeBar} 自钳。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuMeal
 * @since 1.0.0
 */

public record GuHealth(int max, int unitsPerHunger) {

    public int get(ItemStack stack) { return max - TendedGuItem.getState(stack).damageTaken(); }

    void set(TendedGuItem gu, ItemStack stack, int value) {
        gu.store(stack, TendedGuItem.getState(stack).withDamageTaken(max - value));
    }

    boolean takeDamage(TendedGuItem gu, ServerPlayer holder, ItemStack stack, int amount) {
        if (amount <= 0) return false;

        RefinedGuState s = TendedGuItem.getState(stack);
        int taken = Math.min(max, s.damageTaken() + amount);
        gu.store(stack, s.withDamageTaken(taken));
        if (taken < max || VitalGuState.isVital(stack)) return false;

        TendedGuItem.ruin(holder, stack);
        return true;
    }

    void heal(TendedGuItem gu, ItemStack stack, int amount) {
        RefinedGuState s = TendedGuItem.getState(stack);
        if (amount <= 0 || !s.refined()) return;

        gu.store(stack, s.withDamageTaken(Math.max(0, s.damageTaken() - amount)));
    }

    void mendFrom(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) {
        int units = gu.feedUnits(food);
        int hurt = TendedGuItem.getState(stack).damageTaken();
        if (units <= 0 || hurt <= 0) return;

        heal(gu, stack, GuMeal.eat(player, food, hurt, unitsPerHunger, units));
    }
}
