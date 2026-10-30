package net.alex.guzhenren.item.gu.mortal;

import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The recast module [冷却中再催]: its Gu may be driven again inside its effect cooldown, at double the hunger. The
 * buff Gu ({@link TimedBuffModule}) and the Heavenly Mugwort Gu [天蓬蛊] carry it.
 *
 * <p>Only the effect cooldown is waived; the item cooldown and the post-refine guard still hold. What a recast
 * does to the running effect (a buff adds its length, a shield refills) is the effect module's own.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public record RecastModule() implements GuModule {

    private static final int COOLDOWN_HUNGER_MULTIPLIER = 2;

    @Override
    public boolean canUseDuringEffectCooldown() { return true; }

    @Override
    public int getHungerCostMultiplier(MortalGuItem gu, Player player, ItemStack stack) {
        return gu.getEffectCooldownLeft(player, stack) > 0 ? COOLDOWN_HUNGER_MULTIPLIER : 1;
    }
}
