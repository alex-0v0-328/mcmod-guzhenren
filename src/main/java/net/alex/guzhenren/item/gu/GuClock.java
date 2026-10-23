package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

    boolean tickStarvation(ServerPlayer player, ItemStack stack, long days);

    boolean isHungry(ServerPlayer player, ItemStack stack);

    void warn(ServerPlayer player, ItemStack stack, long days);

    boolean tryEat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food);

    int getEssenceAboveHungerFloor(ItemStack stack);

    int getEssencePerHungerPoint();

    void billHungerForEssence(ItemStack stack, int from, int to);

    boolean isSpendForced(ItemStack stack);

    default boolean isSpendForced(ItemStack stack, int multiplier) { return isSpendForced(stack); }

    boolean isBarVisible(ItemStack stack);

    float getBarFraction(ItemStack stack);

    //region 饱食条 -- the boars, Human Jun [人力钧力流], Flower Boar and All-Out Effort
    record HungerBar(int max, int unitsPerHunger, int essencePerHunger, int perUse) implements GuClock {

        private static RefinedGuState getState(ItemStack stack) { return TendedGuItem.getState(stack); }

        private void setHunger(ItemStack stack, int value) {
            RefinedGuState s = getState(stack);
            ModDataComponents.set(stack, ModDataComponents.REFINED_GU_STATE, s.withHunger(Mth.clamp(value, 0, max)));
        }

        private int getHunger(ItemStack stack) { return getState(stack).hunger(); }

        @Override
        public void bind(ServerPlayer player, ItemStack stack) { setHunger(stack, max); }

        @Override
        public boolean tickStarvation(ServerPlayer player, ItemStack stack, long days) {
            if (days <= 0L) return false;

            int left = getHunger(stack) - (int) Math.min(days, max);
            if (left > 0) {
                setHunger(stack, left);
                return false;
            }
            setHunger(stack, 0);
            return !player.getAbilities().instabuild;
        }

        @Override
        public boolean isHungry(ServerPlayer player, ItemStack stack) { return getHunger(stack) <= HUNGRY_THRESHOLD; }

        @Override
        public void warn(ServerPlayer player, ItemStack stack, long days) {
            if (days > 0L) TendedGuItem.announceHungry(player, stack);
        }

        @Override
        public boolean tryEat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) {
            int units = gu.feedUnits(food);
            if (units <= 0) return false;

            int room = max - getHunger(stack);
            if (room <= 0) return false;

            GuMeal meal = GuMeal.createPortion(food.getCount(), room, unitsPerHunger, units);
            if (meal.gained() <= 0) return false;

            if (!player.getAbilities().instabuild) {
                GuMeal.returnEmptyContainers(player, food, meal.eaten());
                food.shrink(meal.eaten());
            }
            setHunger(stack, getHunger(stack) + meal.gained());
            return true;
        }

        @Override
        public int getEssenceAboveHungerFloor(ItemStack stack) {
            return Math.max(0, getHunger(stack) - CHANNEL_HUNGER_FLOOR) * essencePerHunger;
        }

        @Override
        public int getEssencePerHungerPoint() { return essencePerHunger; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {
            setHunger(stack, getHunger(stack) - (to / essencePerHunger - from / essencePerHunger));
        }

        @Override
        public boolean isSpendForced(ItemStack stack) {
            return isSpendForced(stack, 1);
        }

        @Override
        public boolean isSpendForced(ItemStack stack, int multiplier) {
            boolean forced = getHunger(stack) <= 0;
            setHunger(stack, getHunger(stack) - perUse * multiplier);
            return forced;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) { return getHunger(stack) < max; }

        @Override
        public float getBarFraction(ItemStack stack) { return getHunger(stack) / (float) max; }
    }
    //endregion

    //region 无时钟 -- the "reusable, needs no feeding" cell, representable and unbuilt
    record NoClock() implements GuClock {

        @Override
        public void bind(ServerPlayer player, ItemStack stack) {}

        @Override
        public boolean tickStarvation(ServerPlayer player, ItemStack stack, long days) { return false; }

        @Override
        public boolean isHungry(ServerPlayer player, ItemStack stack) { return false; }

        @Override
        public void warn(ServerPlayer player, ItemStack stack, long days) {}

        @Override
        public int getEssenceAboveHungerFloor(ItemStack stack) { return Integer.MAX_VALUE; }

        @Override
        public int getEssencePerHungerPoint() { return Integer.MAX_VALUE; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {}

        @Override
        public boolean isSpendForced(ItemStack stack) { return false; }

        @Override
        public boolean isBarVisible(ItemStack stack) { return false; }

        @Override
        public float getBarFraction(ItemStack stack) { return 0.0F; }

        @Override
        public boolean tryEat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) { return false; }
    }
    //endregion

    int HUNGRY_THRESHOLD = 1;
    int CHANNEL_HUNGER_FLOOR = HUNGRY_THRESHOLD + 1;
}
