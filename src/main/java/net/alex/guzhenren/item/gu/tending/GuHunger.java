package net.alex.guzhenren.item.gu.tending;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 持续照顾型蛊[Tended Gu]的饱食[Hunger]密封接口：由 {@link GuProperties#buildHunger} 按蛊虫规格构造对应实现。
 * <ul>
 *   <li>包含三种实现：{@link HungerBar} 按游戏日扣减饱食；{@link RecoverBar} 用于如故蛊[As Before Gu]，随世界时钟自动回
 *       复满条而不按日扣减；{@link NoClock} 表示永不饥饿（如元老蛊[Primeval Elder Gu]、
 *       天元宝莲[Heavenly Essence Treasure Lotus Gu]、九叶生机草[Nine Leaf Vitality Grass]、更蛊[Watch Gu]与
 *       石窍蛊[Stone Aperture Gu]）。</li>
 *   <li>⚠ {@link #feed} 是唯一的自动进食入口：扫描玩家背包全部槽位（含副手）补充饱食条，仅在 {@code HungerBar} 存在空缺
 *       时触发；食物只补饱食、不治伤势（伤势归 {@link GuHealth}），具体调用时机由 {@code TendedGuItem} 决定。</li>
 *   <li>蛊粮直供：背包吃完仍有空缺、且人在自己当主人的福地里时（{@code BlissfulLands#storageHere}；只在福地里、先背
 *       包，Alex 定），{@link HungerBar#feedFrom} 再从那块福地的库藏一次取一组，照背包规则吃，剩下的放回库藏；
 *       {@link #carriesFood} 在服务端同样算上库藏。</li>
 *   <li>⚠ {@link RecoverBar} 参照 {@code LeafGrowth} 基于 {@code RECOVERED_AT} 时间戳惰性结
 *       算：{@link RecoverBar#getPoints} 仅做推算而不落盘，{@link RecoverBar#settle} 仅在真正长出至少 1 点时才写回组件
 *       （避免随身满条每秒反复写入），日时钟每次心跳也会同步结算随身回复条，最多滞后 1 秒。</li>
 *   <li>⚠ 满条状态下的时间戳按设计会逐渐过期：从满条状态扣减点数时必须立即调用 {@link RecoverBar#restart}（如
 *       {@code AsBeforeGuItem.performOwnUse}），否则下次结算会把旧时间差重新补回。</li>
 *   <li>灌注[Channel]类蛊按轮内进度逐点扣减饱食而非直接消耗真元，每轮对应 {@code perRound} 点饱
 *       食；{@link #getEssenceAboveHungerFloor} 将 {@link #CHANNEL_HUNGER_FLOOR} 之上的饱食点数按轮折算回真元上限，作为
 *       单步灌注的封顶值。</li>
 *   <li>{@link #isShortOfUse} 判断当前饱食是否低于本次催动所需（含增幅倍数），作为触发强行催动[Forced Use]（
 *       {@link GuForcedUse}）的判据；{@link #spendUse} 负责实际扣量并返回是否属于强行扣减；搭档扣量（
 *       {@link #spendAsPartner}）所支援的催动已通过主蛊自检且无独立警告，因此仅在空条时判定为强行扣减。</li>
 *   <li>⚠ 「饥饿」特指下次跨日扣量时将饿死的状态：判定阈值为每日扣量（{@code perDay}）而非单顿食量；只有
 *       {@code HungerBar} 会产生饥饿状态，提示警告由 {@code TendedGuItem.feedAndWarn} 负责。</li>
 *   <li>本文件有意超过 300 行：密封接口与其三个 {@code record} 实现共用背包扫描与单位换算辅助逻辑，并排维护可读性最
 *       佳。</li>
 * </ul>
 */

public sealed interface GuHunger {

    void bind(ServerPlayer player, ItemStack stack);

    boolean tickStarvation(ServerPlayer player, ItemStack stack, long days);

    boolean isHungry(ItemStack stack);

    void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack);

    int getEssenceAboveHungerFloor(ItemStack stack);

    void billHungerForEssence(ItemStack stack, int from, int to);

    boolean isShortOfUse(ItemStack stack, int multiplier);

    boolean spendUse(ItemStack stack, int multiplier);

    default boolean spendAsPartner(ItemStack stack) { return spendUse(stack, 1); }

    boolean isBarVisible(ItemStack stack);

    float getBarFraction(ItemStack stack);

    //region the bag [背包]
    default boolean carriesFood(TendedGuItem gu, Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (gu.feedUnits(inventory.getItem(slot)) > 0) return true;
        }
        BlissfulStorage storage = player instanceof ServerPlayer server ? BlissfulLands.storageHere(server) : null;
        return storage != null && !storage.first(food -> gu.feedUnits(food) > 0).isEmpty();
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

        @Override
        public void feed(TendedGuItem gu, ServerPlayer player, ItemStack stack) {
            if (getHunger(stack) >= max) return;

            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack food = inventory.getItem(slot);
                if (gu.feedUnits(food) <= 0) continue;

                eat(gu, player, stack, food);
            }
            BlissfulStorage storage = BlissfulLands.storageHere(player);
            if (storage != null) feedFrom(gu, player, stack, storage);
        }

        public void feedFrom(TendedGuItem gu, ServerPlayer player, ItemStack stack, BlissfulStorage storage) {
            while (getHunger(stack) < max) {
                int index = foodIndex(gu, storage);
                if (index < 0) return;

                ItemStack food = storage.extract(index, Integer.MAX_VALUE);
                int before = food.getCount();
                eat(gu, player, stack, food);
                boolean ate = food.getCount() < before;
                if (!food.isEmpty()) storage.insert(food);
                if (!ate) return;
            }
        }

        private static int foodIndex(TendedGuItem gu, BlissfulStorage storage) {
            List<BlissfulStorage.Entry> entries = storage.entries();
            for (int i = 0; i < entries.size(); i++) {
                if (gu.feedUnits(entries.get(i).key()) > 0) return i;
            }
            return -1;
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
        public boolean spendUse(ItemStack stack, int multiplier) {
            boolean forced = isShortOfUse(stack, multiplier);
            setHunger(stack, getHunger(stack) - perUse * multiplier);
            return forced;
        }

        @Override
        public boolean spendAsPartner(ItemStack stack) {
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
        public boolean spendUse(ItemStack stack, int multiplier) {
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
        public boolean spendUse(ItemStack stack, int multiplier) { return false; }

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
