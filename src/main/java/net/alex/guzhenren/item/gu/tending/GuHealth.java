package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 持续照顾型蛊[Tended Gu]的生命值规格：{@link RefinedGuState} 中存储已受伤害而非当前生命，未动过的野生蛊默认视为满血。
 * <ul>
 *   <li>最大生命值由 {@link GuProperties#buildHealth} 按 {@code 4 × 转数 + 4}（即 8 至 24）生成，不复用玩家的
 *       {@code Rank.getMaxHealth}；生命归零时触发损毁（{@code TendedGuItem.ruin}），但本命蛊[Vital Gu]例外，0 生命仍保
 *       留在本命槽中。</li>
 *   <li>食物按饱食[Hunger]点数等比折算修复伤势，固定 1 点生命等于 1 点饱食（{@code unitsPerHunger}）；{@link #mendFrom}
 *       仅消耗整件食物且不会超出当前伤势上限。</li>
 *   <li>⚠ 除 {@code RecoverBar.settleHealth} 直接缩减已钳制的伤势外，所有生命写入均须经过宿主 {@code store} 对
 *       {@code damageTaken} 做范围钳制；饱食条则由 {@code GuHunger.storeBar} 自行钳制。</li>
 * </ul>
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
