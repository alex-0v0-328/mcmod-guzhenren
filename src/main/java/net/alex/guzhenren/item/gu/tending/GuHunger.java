package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 持续照顾型蛊[Tended Gu]的饱食[Hunger]密封接口，由 {@link GuProperties#buildHunger} 构建。
 * <ul>
 *   <li>{@link HungerBar} 按日扣量；{@link RecoverBar} 如故蛊[As Before Gu]，随世界时钟回满，不按日扣；{@link NoClock}
 *       不饥饿：元老蛊[Primeval Elder Gu]、天元宝莲[Heavenly Essence Treasure Lotus Gu]、
 *       九叶生机草[Nine Leaf Vitality Grass]、更蛊[Watch Gu]、石窍蛊[Stone Aperture Gu]。</li>
 *   <li>⚠ {@link #feed} 是唯一进食：吃背包全槽位（含副手），每份先经 {@link GuHealth} 修复再补条，仅 {@code HungerBar}
 *       有空位时运行；满条不为修复而吃，伤等下次扣量；时机由 {@code TendedGuItem} 定。</li>
 *   <li>⚠ 回复条如 {@code LeafGrowth} 从 {@code RECOVERED_AT} 惰性结算：{@link RecoverBar#getPoints(ItemStack, long)}
 *       只推算，{@link RecoverBar#settle} 长出一点才写（随身满条不每秒写）；日时钟每次心跳结算随身回复条，至多滞后
 *       1 秒。</li>
 *   <li>⚠ 满条时间戳按设计过期：从满条扣点须当即 {@link RecoverBar#restart}（{@code AsBeforeGuItem.performOwnUse}），
 *       否则下次结算把点还回。</li>
 *   <li>回复条同时钟修复生命，每 {@code ticksPerPoint} 一点，依 {@code HEALED_AT}：受伤即起，伤愈即删，无伤不写。</li>
 *   <li>灌注[Channel]类蛊按轮进度逐点扣饱食而非真元[Essence]，每轮 {@code perRound} 点；
 *       {@link #getEssenceAboveHungerFloor} 把 {@link #CHANNEL_HUNGER_FLOOR} 之上的点数按轮折回真元，作每步上限。</li>
 *   <li>{@link #isShortOfUse}：条少于本次催动（含倍数），即强行催动[Forced Use]（{@link GuForcedUse}）判据；
 *       {@link #isSpendForced} 扣量并同答；搭档扣量（{@link #isSpendForcedAsPartner}）沿用空条规则。</li>
 *   <li>⚠ 「饥饿」仅指下次日切会饿死：阈值为每日扣量（{@code perDay}），非一顿；只有 {@code HungerBar} 会饥饿，
 *       警告归 {@code TendedGuItem.feedAndWarn}。</li>
 *   <li>有意超 300 行：密封接口与三个 record 共用背包与单位换算，并排最好读。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GuProperties
 * @see TendedGuItem
 * @since 1.0.0
 */

public sealed interface GuHunger {

    void bind(ServerPlayer player, ItemStack stack);

    boolean tickStarvation(ServerPlayer player, ItemStack stack, long days);

    boolean isHungry(ItemStack stack);

    void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack);

    int getEssenceAboveHungerFloor(ItemStack stack);

    void billHungerForEssence(ItemStack stack, int from, int to);

    boolean isShortOfUse(ItemStack stack, int multiplier);

    boolean isSpendForced(ItemStack stack, int multiplier);

    default boolean isSpendForcedAsPartner(ItemStack stack) { return isSpendForced(stack, 1); }

    boolean isBarVisible(ItemStack stack);

    float getBarFraction(ItemStack stack);

    //region the bag [背包]
    default boolean carriesFood(TendedGuItem gu, Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (gu.feedUnits(inventory.getItem(slot)) > 0) return true;
        }
        return false;
    }
    //endregion

    //region the hunger bar [饱食条]
    record HungerBar(int max, int unitsPerHunger, int essencePerRound, int perRound, int perUse, int perDay)
            implements GuHunger {

        private void setHunger(ItemStack stack, int value) { storeBar(stack, value, max); }

        private int getHunger(ItemStack stack) { return TendedGuItem.getState(stack).hunger(); }

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
        public boolean isHungry(ItemStack stack) { return getHunger(stack) <= perDay; }

        // Every meal mends first, then fills the bar, and only a bar with room eats.
        @Override
        public void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack) {
            if (getHunger(stack) >= max) return;

            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack food = inventory.getItem(slot);
                if (gu.feedUnits(food) <= 0) continue;

                gu.health.mendFrom(gu, player, stack, food);
                eat(gu, player, stack, food);
            }
        }

        private void eat(TendedGuItem gu, ServerPlayer player, ItemStack stack, ItemStack food) {
            int units = gu.feedUnits(food);
            int room = max - getHunger(stack);
            if (units <= 0 || room <= 0) return;

            int gained = GuMeal.eat(player, food, room, unitsPerHunger, units);
            if (gained > 0) setHunger(stack, getHunger(stack) + gained);
        }

        @Override
        public int getEssenceAboveHungerFloor(ItemStack stack) {
            return Math.max(0, getHunger(stack) - CHANNEL_HUNGER_FLOOR) * (essencePerRound / perRound);
        }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {
            setHunger(stack, getHunger(stack) - (getRoundPoints(to) - getRoundPoints(from)));
        }

        private int getRoundPoints(int invested) { return (int) ((long) invested * perRound / essencePerRound); }

        @Override
        public boolean isShortOfUse(ItemStack stack, int multiplier) { return getHunger(stack) < perUse * multiplier; }

        @Override
        public boolean isSpendForced(ItemStack stack, int multiplier) {
            boolean forced = isShortOfUse(stack, multiplier);
            setHunger(stack, getHunger(stack) - perUse * multiplier);
            return forced;
        }

        // A partner backs a use that already passed its own gate, with no warning of its own, so only an empty bar
        // forces it.
        @Override
        public boolean isSpendForcedAsPartner(ItemStack stack) {
            boolean forced = getHunger(stack) <= 0;
            setHunger(stack, getHunger(stack) - perUse);
            return forced;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) { return getHunger(stack) < max; }

        @Override
        public float getBarFraction(ItemStack stack) { return getHunger(stack) / (float) max; }
    }
    //endregion

    //region the recover bar [回复条]
    record RecoverBar(int max, int ticksPerPoint, int perUse) implements GuHunger {

        private void setPoints(ItemStack stack, int value) { storeBar(stack, value, max); }

        public int getPoints(ItemStack stack) { return TendedGuItem.getState(stack).hunger(); }

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
            RefinedGuState s = TendedGuItem.getState(stack);
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
            long now = player.level().getGameTime();
            settle(stack, now);
            settleHealth(stack, now);
            return false;
        }

        @Override
        public boolean isHungry(ItemStack stack) { return false; }

        @Override
        public void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack) {}

        @Override
        public int getEssenceAboveHungerFloor(ItemStack stack) { return Integer.MAX_VALUE; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {}

        @Override
        public boolean isShortOfUse(ItemStack stack, int multiplier) { return getPoints(stack) < perUse; }

        @Override
        public boolean isSpendForced(ItemStack stack, int multiplier) {
            boolean forced = isShortOfUse(stack, multiplier);
            setPoints(stack, getPoints(stack) - perUse);
            return forced;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) { return getPoints(stack) < max; }

        @Override
        public float getBarFraction(ItemStack stack) { return getPoints(stack) / (float) max; }
    }
    //endregion

    //region no clock [无时钟]
    record NoClock() implements GuHunger {

        @Override
        public void bind(ServerPlayer player, ItemStack stack) {}

        @Override
        public boolean tickStarvation(ServerPlayer player, ItemStack stack, long days) { return false; }

        @Override
        public boolean isHungry(ItemStack stack) { return false; }

        @Override
        public void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack) {}

        @Override
        public int getEssenceAboveHungerFloor(ItemStack stack) { return Integer.MAX_VALUE; }

        @Override
        public void billHungerForEssence(ItemStack stack, int from, int to) {}

        @Override
        public boolean isShortOfUse(ItemStack stack, int multiplier) { return false; }

        @Override
        public boolean isSpendForced(ItemStack stack, int multiplier) { return false; }

        @Override
        public boolean isBarVisible(ItemStack stack) { return false; }

        @Override
        public float getBarFraction(ItemStack stack) { return 0.0F; }
    }
    //endregion

    int HUNGRY_THRESHOLD = 1;
    int CHANNEL_HUNGER_FLOOR = HUNGRY_THRESHOLD + 1;

    private static void storeBar(ItemStack stack, int value, int max) {
        RefinedGuState state = TendedGuItem.getState(stack);
        stack.set(ModDataComponents.REFINED_GU_STATE, state.withHunger(Mth.clamp(value, 0, max)));
    }
}
