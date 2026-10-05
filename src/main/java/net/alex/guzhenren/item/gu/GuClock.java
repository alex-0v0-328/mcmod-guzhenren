package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * How a tended Gu [需照顾] is billed over time, and what it takes to keep one alive.
 *
 * <p>A sealed interface with two implementations: {@link HungerBar} (the boars, human-jun, buff Gu,
 * all-out-effort, liquor worm, zombies -- anyone with a bar that drains), and {@link NoClock} (primeval
 * elder Gu -- never hungry, never feeds). Built by {@link GuSpec#buildClock} at construction.
 *
 * <p>⚠ "Hungry" means only that the next day rollover will kill it, and the threshold is flat at 1
 * for every Gu on a bar. It must never be scaled to the size of the meal.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @see TendedGuItem
 * @since 1.0.0
 */

public sealed interface GuClock {

    void bind(ServerPlayer player, ItemStack stack);

    boolean starves(ServerPlayer player, ItemStack stack, long days);

    boolean hungry(ServerPlayer player, ItemStack stack);

    void warn(ServerPlayer player, ItemStack stack, long days);

    boolean eat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food);

    int essenceAboveHungerFloor(ItemStack stack);

    int essencePerHungerPoint();

    void billHungerForEssence(ItemStack stack, int from, int to);

    boolean spendWasForced(ItemStack stack);

    default boolean spendWasForced(ItemStack stack, int multiplier) { return spendWasForced(stack); }

    boolean barVisible(ItemStack stack);

    float barFraction(ItemStack stack);

    //region 饱食条 -- the boars, Human Jun [人力钧力流], Flower Boar and All-Out Effort
    record HungerBar(int max, int unitsPerHunger, int essencePerHunger, int perUse) implements GuClock {

        private static RefinedGuState state(ItemStack stack) { return TendedGuItem.state(stack); }

        private void setHunger(ItemStack stack, int value) {
            RefinedGuState s = state(stack);
            stack.set(ModDataComponents.REFINED_GU_STATE.get(), s.withHunger(Math.clamp(value, 0, max)));
        }

        private int hunger(ItemStack stack) { return state(stack).hunger(); }

        @Override
        public void bind(ServerPlayer player, ItemStack stack) { setHunger(stack, max); }

        @Override
        public boolean starves(ServerPlayer player, ItemStack stack, long days) {
            if (days <= 0L) return false;

            int left = hunger(stack) - (int) Math.min(days, max);
            if (left > 0) {
                setHunger(stack, left);
                return false;
            }
            setHunger(stack, 0);
            return !player.hasInfiniteMaterials();
        }

        @Override
        public boolean hungry(ServerPlayer player, ItemStack stack) { return hunger(stack) <= HUNGRY_THRESHOLD; }

        @Override
        public void warn(ServerPlayer player, ItemStack stack, long days) {
            if (days > 0L) TendedGuItem.announceHungry(player, stack);
        }

        @Override
        public boolean eat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) {
            int units = gu.feedUnits(food);
            if (units <= 0) return false;

            int room = max - hunger(stack);
            if (room <= 0) return false;

            GuMeal meal = GuMeal.portion(food.getCount(), room, unitsPerHunger, units);
            if (meal.gained() <= 0) return false;

            if (!player.hasInfiniteMaterials()) {
                GuMeal.returnEmptyContainers(player, food, meal.eaten());
                food.shrink(meal.eaten());
            }
            setHunger(stack, hunger(stack) + meal.gained());
            return true;
        }

        @Override
        public int essenceAboveHungerFloor(ItemStack stack) {
            return Math.max(0, hunger(stack) - CHANNEL_HUNGER_FLOOR) * essencePerHunger;
        }

        @Override
        public int essencePerHungerPoint() { return essencePerHunger; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {
            setHunger(stack, hunger(stack) - (to / essencePerHunger - from / essencePerHunger));
        }

        @Override
        public boolean spendWasForced(ItemStack stack) {
            return spendWasForced(stack, 1);
        }

        @Override
        public boolean spendWasForced(ItemStack stack, int multiplier) {
            boolean forced = hunger(stack) <= 0;
            setHunger(stack, hunger(stack) - perUse * multiplier);
            return forced;
        }

        @Override
        public boolean barVisible(ItemStack stack) { return hunger(stack) < max; }

        @Override
        public float barFraction(ItemStack stack) { return hunger(stack) / (float) max; }
    }
    //endregion

    //region 无时钟 -- the "reusable, needs no feeding" cell, representable and unbuilt
    record NoClock() implements GuClock {

        @Override
        public void bind(ServerPlayer player, ItemStack stack) {}

        @Override
        public boolean starves(ServerPlayer player, ItemStack stack, long days) { return false; }

        @Override
        public boolean hungry(ServerPlayer player, ItemStack stack) { return false; }

        @Override
        public void warn(ServerPlayer player, ItemStack stack, long days) {}

        @Override
        public int essenceAboveHungerFloor(ItemStack stack) { return Integer.MAX_VALUE; }

        @Override
        public int essencePerHungerPoint() { return Integer.MAX_VALUE; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {}

        @Override
        public boolean spendWasForced(ItemStack stack) { return false; }

        @Override
        public boolean barVisible(ItemStack stack) { return false; }

        @Override
        public float barFraction(ItemStack stack) { return 0.0F; }

        @Override
        public boolean eat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) { return false; }
    }
    //endregion

    int HUNGRY_THRESHOLD = 1;
    int CHANNEL_HUNGER_FLOOR = HUNGRY_THRESHOLD + 1;
}
