package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The recast behavior [冷却中再催]: its Gu may be driven again inside its effect cooldown, at double the hunger. The
 * buff Gu ({@link TimedBuffBehavior}) and the Heavenly Mugwort Gu [天蓬蛊] carry it.
 *
 * <p>Only the effect cooldown is waived; the item cooldown and the post-refine guard still hold. What a recast
 * does to the running effect (a buff adds its length, a shield refills) is the effect behavior's own.
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
