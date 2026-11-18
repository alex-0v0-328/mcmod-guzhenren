package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 允许在效果冷却期间再次催动的组合行为：效果冷却未完时仍放行催动，但当次催动的饱食[Hunger]消耗翻倍（物品冷却与炼化后保
 * 护窗口照常生效）；限时增益蛊（{@link TimedBuffBehavior}）与天蓬蛊[Heavenly Mugwort Gu]挂载本行为，重复催动对运行中效
 * 果的具体刷新规则（增益叠加时长、护盾回满）由各自的效果行为处理。
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
