package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * How a tended Gu [需照顾] is billed over time, and what it takes to keep one alive.
 *
 * <p>A sealed interface with three implementations: {@link HungerBar} (every tended Gu with a bar that drains),
 * {@link RecoverBar} (the As Before Gu [如故蛊]: a bar that refills with the world clock, never drains by the day,
 * never eats) and {@link NoClock} (Primeval Elder Gu, Heavenly Essence Treasure Lotus Gu, Nine Leaf Vitality Grass,
 * Watch Gu and Stone Aperture Gu -- never hungry, never feeds). Built by {@link GuSpec#buildClock} at construction.
 *
 * <p>⚠ {@code RecoverBar} settles lazily from its {@code RECOVERED_AT} stamp, like {@code LeafGrowth}: {@link
 * RecoverBar#getPoints(ItemStack, long)} derives the count at any game time without writing, {@link
 * RecoverBar#settle} writes only a grown point (a full bar is left alone, so a carried one costs no stack write a
 * second). The day-clock walk settles every carried one each heartbeat, so the stored count is never more than a
 * second stale. ⚠ A full bar's stamp is stale by design, so a use that spends from a full bar must {@link
 * RecoverBar#restart} the stamp at the spend ({@code AsBeforeGuItem.useApply}), or the next settle would hand the
 * point straight back.
 *
 * <p>⚠ "Hungry" means only that the next day rollover will kill it: the threshold is the bar's daily drain
 * ({@code perDay}), never the size of a meal.
 *
 * <p>⚠ {@code HungerBar.warn} latches on {@code days > 0}: {@link GuUpkeep} runs on every heartbeat while the bar
 * drains only on a day rollover, so without the latch the hungry message would repeat every second.
 * {@code TendedGuItem.drive} passes 1 for its one immediate warning.
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

    //region 饱食条 -- every tended Gu whose bar drains
    record HungerBar(int max, int unitsPerHunger, int essencePerHunger, int perUse, int perDay) implements GuClock {

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

            int left = getHunger(stack) - (int) Math.min(days * perDay, max);
            if (left > 0) {
                setHunger(stack, left);
                return false;
            }
            setHunger(stack, 0);
            return !player.getAbilities().instabuild;
        }

        @Override
        public boolean isHungry(ServerPlayer player, ItemStack stack) { return getHunger(stack) <= perDay; }

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

    //region 回复条 -- refills with the world clock, never drains by the day, never eats
    record RecoverBar(int max, int ticksPerPoint, int perUse) implements GuClock {

        private static RefinedGuState getState(ItemStack stack) { return TendedGuItem.getState(stack); }

        private void setPoints(ItemStack stack, int value) {
            RefinedGuState s = getState(stack);
            ModDataComponents.set(stack, ModDataComponents.REFINED_GU_STATE, s.withHunger(Mth.clamp(value, 0, max)));
        }

        public int getPoints(ItemStack stack) { return getState(stack).hunger(); }

        public int getPoints(ItemStack stack, long now) {
            Long stamp = ModDataComponents.get(stack, ModDataComponents.RECOVERED_AT);
            int points = getPoints(stack);
            if (stamp == null || now < stamp || points >= max) return points;
            return (int) Math.min(max, points + (now - stamp) / ticksPerPoint);
        }

        public int getTicksToNextPoint(ItemStack stack, long now) {
            Long stamp = ModDataComponents.get(stack, ModDataComponents.RECOVERED_AT);
            if (getPoints(stack, now) >= max) return 0;
            if (stamp == null || now < stamp) return ticksPerPoint;
            return (int) (ticksPerPoint - (now - stamp) % ticksPerPoint);
        }

        public void settle(ItemStack stack, long now) {
            Long stamp = ModDataComponents.get(stack, ModDataComponents.RECOVERED_AT);
            if (stamp == null || now < stamp) {
                ModDataComponents.set(stack, ModDataComponents.RECOVERED_AT, now);
                return;
            }
            int points = getPoints(stack);
            if (points >= max) return;

            long grown = (now - stamp) / ticksPerPoint;
            if (grown <= 0L) return;

            if (points + grown >= max) {
                setPoints(stack, max);
                ModDataComponents.set(stack, ModDataComponents.RECOVERED_AT, now);
                return;
            }
            setPoints(stack, points + (int) grown);
            ModDataComponents.set(stack, ModDataComponents.RECOVERED_AT, stamp + grown * ticksPerPoint);
        }

        public void restart(ItemStack stack, long now) {
            ModDataComponents.set(stack, ModDataComponents.RECOVERED_AT, now);
        }

        @Override
        public void bind(ServerPlayer player, ItemStack stack) {
            setPoints(stack, max);
            restart(stack, player.level().getGameTime());
        }

        @Override
        public boolean tickStarvation(ServerPlayer player, ItemStack stack, long days) {
            settle(stack, player.level().getGameTime());
            return false;
        }

        @Override
        public boolean isHungry(ServerPlayer player, ItemStack stack) { return false; }

        @Override
        public void warn(ServerPlayer player, ItemStack stack, long days) {}

        @Override
        public boolean tryEat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) { return false; }

        @Override
        public int getEssenceAboveHungerFloor(ItemStack stack) { return Integer.MAX_VALUE; }

        @Override
        public int getEssencePerHungerPoint() { return Integer.MAX_VALUE; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {}

        @Override
        public boolean isSpendForced(ItemStack stack) {
            boolean forced = getPoints(stack) < perUse;
            setPoints(stack, getPoints(stack) - perUse);
            return forced;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) { return getPoints(stack) < max; }

        @Override
        public float getBarFraction(ItemStack stack) { return getPoints(stack) / (float) max; }
    }
    //endregion

    //region 无时钟 -- the "reusable, needs no feeding" cell
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
