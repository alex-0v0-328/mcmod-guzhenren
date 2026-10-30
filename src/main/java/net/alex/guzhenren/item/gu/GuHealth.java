package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.item.GuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A tended Gu's health [蛊虫生命值], stored as damage TAKEN on {@link RefinedGuState}, so an untouched wild Gu reads
 * as full. Built by {@link GuSpec#buildHealth} from {@code GuSpec.health}, {@code TendedGuItem.GU_MAX_HEALTH} unless
 * the spec sets it; it stays off {@code Rank}, whose {@code getMaxHealth} is the player's.
 *
 * <p>Food mends at the hunger rate: one health point costs what one hunger point does ({@code unitsPerHunger}), and
 * {@link #mendFrom} eats whole items only, never past the wound. A Gu at zero health is ruined
 * ({@code TendedGuItem.ruin}) unless it is a Vital Gu [本命蛊], which keeps its slot at zero.
 *
 * <p>⚠ Every write goes through the host's {@code store}, which clamps the whole state; a bare component write
 * would skip the clamp on the other fields.
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
        if (taken < max || GuItem.isVital(stack)) return false;

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

        GuMeal meal = GuMeal.createPortion(food.getCount(), hurt, unitsPerHunger, units);
        if (meal.gained() <= 0) return;

        if (!player.getAbilities().instabuild) {
            GuMeal.returnEmptyContainers(player, food, meal.eaten());
            food.shrink(meal.eaten());
        }
        heal(gu, stack, meal.gained());
    }
}
