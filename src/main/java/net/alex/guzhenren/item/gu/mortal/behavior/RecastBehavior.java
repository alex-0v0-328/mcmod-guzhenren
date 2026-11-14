package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 冷却中再催行为。效果冷却内可再催，饱食[Hunger]翻倍；增益蛊（{@link TimedBuffBehavior}）与天蓬蛊[Heavenly Mugwort Gu]
 * 带此行为；只免效果冷却，物品冷却与炼化后保护照旧；再催对运行中效果的作用（增益加时长、护盾回满）归效果行为。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
 */

public record RecastBehavior() implements GuBehavior {

    private static final int COOLDOWN_HUNGER_MULTIPLIER = 2;

    @Override
    public boolean canUseDuringEffectCooldown() { return true; }

    @Override
    public int getHungerCostMultiplier(MortalGuItem gu, Player player, ItemStack stack) {
        return gu.getEffectCooldownLeft(player, stack) > 0 ? COOLDOWN_HUNGER_MULTIPLIER : 1;
    }
}
