package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A tended Gu's hunger [饱食]: how it is billed over time, what it takes to keep one alive, and the meals it takes
 * from the holder's bag ({@link #feed}: every slot, offhand included; each food mends first through {@link GuHealth},
 * then fills the bar).
 *
 * <p>⚠ {@link #feed} is the one meal and runs only while the bar has room ({@link #hasRoom}): a full bar never eats
 * just to mend, so a wound waits for the next drop. When it runs is {@code TendedGuItem}'s rule, not this one's.
 *
 * <p>A sealed interface with three implementations: {@link HungerBar} (every tended Gu with a bar that drains),
 * {@link RecoverBar} (the As Before Gu [如故蛊]: a bar that refills with the world clock, never drains by the day,
 * never eats) and {@link NoClock} (Primeval Elder Gu, Heavenly Essence Treasure Lotus Gu, Nine Leaf Vitality Grass,
 * Watch Gu and Stone Aperture Gu -- never hungry, never feeds). Built by {@link GuSpec#buildHunger} at construction.
 *
 * <p>⚠ {@code RecoverBar} settles lazily from its {@code RECOVERED_AT} stamp, like {@code LeafGrowth}: {@link
 * RecoverBar#getPoints(ItemStack, long)} derives the count at any game time without writing, {@link
 * RecoverBar#settle} writes only a grown point (a full bar is left alone, so a carried one costs no stack write a
 * second). The day-clock walk settles every carried one each heartbeat, so the stored count is never more than a
 * second stale. ⚠ A full bar's stamp is stale by design, so a use that spends from a full bar must {@link
 * RecoverBar#restart} the stamp at the spend ({@code AsBeforeGuItem.performOwnUse}), or the next settle would hand the
 * point straight back.
 *
 * <p>{@code RecoverBar} mends health on the same clock, one point a {@code ticksPerPoint}, from its own {@code
 * HEALED_AT} stamp: started by the first settle that finds it hurt, removed once whole, so a whole one costs no write.
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

public sealed interface GuHunger {

    void bind(ServerPlayer player, ItemStack stack);

    boolean tickStarvation(ServerPlayer player, ItemStack stack, long days);

    boolean isHungry(ServerPlayer player, ItemStack stack);

    void warn(ServerPlayer player, ItemStack stack, long days);

    boolean tryEat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food);

    boolean hasRoom(ItemStack stack);

    int getEssenceAboveHungerFloor(ItemStack stack);

    int getEssencePerHungerPoint();

    void billHungerForEssence(ItemStack stack, int from, int to);

    boolean isSpendForced(ItemStack stack);

    default boolean isSpendForced(ItemStack stack, int multiplier) { return isSpendForced(stack); }

    boolean isBarVisible(ItemStack stack);

    float getBarFraction(ItemStack stack);

    //region the bag -- every meal mends first, then fills the bar, and only a bar with room eats
    default void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack) {
        if (!hasRoom(stack)) return;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack food = inventory.getItem(slot);
            if (gu.feedUnits(food) <= 0) continue;

            gu.health.mendFrom(gu, player, stack, food);
            tryEat(gu, player, stack, food);
        }
    }

    default boolean carriesFood(TendedGuItem gu, Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (gu.feedUnits(inventory.getItem(slot)) > 0) return true;
        }
        return false;
    }
    //endregion

    //region 饱食条 -- every tended Gu whose bar drains
    record HungerBar(int max, int unitsPerHunger, int essencePerHunger, int perUse, int perDay) implements GuHunger {

        private static RefinedGuState getState(ItemStack stack) { return TendedGuItem.getState(stack); }

        private void setHunger(ItemStack stack, int value) {
            RefinedGuState s = getState(stack);
            stack.set(ModDataComponents.REFINED_GU_STATE, s.withHunger(Mth.clamp(value, 0, max)));
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
        public boolean hasRoom(ItemStack stack) { return getHunger(stack) < max; }

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
    record RecoverBar(int max, int ticksPerPoint, int perUse) implements GuHunger {

        private static RefinedGuState getState(ItemStack stack) { return TendedGuItem.getState(stack); }

        private void setPoints(ItemStack stack, int value) {
            RefinedGuState s = getState(stack);
            stack.set(ModDataComponents.REFINED_GU_STATE, s.withHunger(Mth.clamp(value, 0, max)));
        }

        public int getPoints(ItemStack stack) { return getState(stack).hunger(); }

        public int getPoints(ItemStack stack, long now) {
            Long stamp = stack.get(ModDataComponents.RECOVERED_AT);
            int points = getPoints(stack);
            if (stamp == null || now < stamp || points >= max) return points;
            return (int) Math.min(max, points + (now - stamp) / ticksPerPoint);
        }

        public int getTicksToNextPoint(ItemStack stack, long now) {
            Long stamp = stack.get(ModDataComponents.RECOVERED_AT);
            if (getPoints(stack, now) >= max) return 0;
            if (stamp == null || now < stamp) return ticksPerPoint;
            return (int) (ticksPerPoint - (now - stamp) % ticksPerPoint);
        }

        public void settle(ItemStack stack, long now) {
            Long stamp = stack.get(ModDataComponents.RECOVERED_AT);
            if (stamp == null || now < stamp) {
                stack.set(ModDataComponents.RECOVERED_AT, now);
                return;
            }
            int points = getPoints(stack);
            if (points >= max) return;

            long grown = (now - stamp) / ticksPerPoint;
            if (grown <= 0L) return;

            if (points + grown >= max) {
                setPoints(stack, max);
                stack.set(ModDataComponents.RECOVERED_AT, now);
                return;
            }
            setPoints(stack, points + (int) grown);
            stack.set(ModDataComponents.RECOVERED_AT, stamp + grown * ticksPerPoint);
        }

        public void restart(ItemStack stack, long now) {
            stack.set(ModDataComponents.RECOVERED_AT, now);
        }

        public void settleHealth(ItemStack stack, long now) {
            RefinedGuState s = getState(stack);
            Long stamp = stack.get(ModDataComponents.HEALED_AT);
            if (s.damageTaken() <= 0) {
                if (stamp != null) stack.remove(ModDataComponents.HEALED_AT);
                return;
            }
            if (stamp == null || now < stamp) {
                stack.set(ModDataComponents.HEALED_AT, now);
                return;
            }
            long grown = (now - stamp) / ticksPerPoint;
            if (grown <= 0L) return;

            int left = (int) Math.max(0L, s.damageTaken() - grown);
            stack.set(ModDataComponents.REFINED_GU_STATE, s.withDamageTaken(left));
            if (left == 0) {
                stack.remove(ModDataComponents.HEALED_AT);
            } else {
                stack.set(ModDataComponents.HEALED_AT, stamp + grown * ticksPerPoint);
            }
        }

        @Override
        public void bind(ServerPlayer player, ItemStack stack) {
            setPoints(stack, max);
            restart(stack, player.level().getGameTime());
        }

        @Override
        public boolean tickStarvation(ServerPlayer player, ItemStack stack, long days) {
            settle(stack, player.level().getGameTime());
            settleHealth(stack, player.level().getGameTime());
            return false;
        }

        @Override
        public boolean isHungry(ServerPlayer player, ItemStack stack) { return false; }

        @Override
        public void warn(ServerPlayer player, ItemStack stack, long days) {}

        @Override
        public boolean tryEat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) { return false; }

        @Override
        public boolean hasRoom(ItemStack stack) { return false; }

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
    record NoClock() implements GuHunger {

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

        @Override
        public boolean hasRoom(ItemStack stack) { return false; }
    }
    //endregion

    int HUNGRY_THRESHOLD = 1;
    int CHANNEL_HUNGER_FLOOR = HUNGRY_THRESHOLD + 1;
}
