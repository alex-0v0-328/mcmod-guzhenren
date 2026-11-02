package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A tended Gu's health [蛊虫生命值], stored as damage TAKEN on {@link RefinedGuState}, so an untouched wild Gu reads
 * as full. Built by {@link GuSpec#buildHealth} from the Gu's rank (4 x rank + 4, so 8 to
 * 24); it stays off {@code Rank}, whose {@code getMaxHealth} is the player's.
 *
 * <p>Food mends at the hunger rate: one health point costs what one hunger point does ({@code unitsPerHunger}), and
 * {@link #mendFrom} eats whole items only, never past the wound. A Gu at zero health is ruined
 * ({@code TendedGuItem.ruin}) unless it is a Vital Gu [本命蛊], which keeps its slot at zero.
 *
 * <p>⚠ Every health write goes through the host's {@code store}, which clamps {@code damageTaken}, except {@code
 * RecoverBar.settleHealth}, which only shrinks an already clamped wound; the hunger bar clamps its own writes in
 * {@code GuHunger.storeBar}.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuMeal
 * @since 1.0.0
 */

public record GuHealth(int max, int unitsPerHunger) {

    public int get(ItemStack stack) { return max - TendedGuItem.getState(stack).damageTaken(); }

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
