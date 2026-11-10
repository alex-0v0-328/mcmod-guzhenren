package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A tended Gu's channeling quota [灌注]: how much essence one step may pour, for a channeled Gu's round and for the
 * refining of a wild one. The host ({@code TendedGuItem}) keeps the hooks, the hunger meal and the payment; this
 * only sizes a step and writes what was poured.
 *
 * <p>⚠ Refining and channeling advance one step every {@link #STEP_TICKS} (2 ticks), at two paces that stay separate.
 * A holder above the Gu's rank is Gu-paced: a channel step pours at most 1/{@link #STEPS_PER_ROUND} of a round, a
 * round every {@link #ROUND_TICKS} (8 steps, 16 ticks = 0.8 s) scaled by {@code TimeFlow}, whatever the hunger, and a
 * refining step may pour the whole pool. At the Gu's rank or below, the pool sets the pace over the same 8-step
 * cycle: 1/9, 1/8, ... then 1/2 of the pool per step, 8/9 of it a cycle at normal time flow.
 *
 * <p>⚠ A step is sized from the pool, which is never below {@link #ESSENCE_FLOOR} when it pours: below it a step is
 * 0. Creative draws no stone, so its top-up never lifts a short pool; it sizes from the pool the top-up would have
 * left ({@code PrimevalStoneSupply.getPoolAfterTopUp}), never below the floor, and it ignores the hunger floor
 * ({@link #getEssenceAboveHungerFloor}). ☠ Sized from the bare pool, a creative step on an empty one is a ninth of
 * the floor, 2 a step against a rank three refining of 51,200. A channel step is capped by the essence above the
 * hunger floor, so the host eats before it sizes; it never pours past the end of the round.
 *
 * <p>Every sizing method has a pure overload taking only numbers, like {@code TimeFlow}'s arithmetic, for the tests;
 * the player overloads read those numbers and call it, and consume the one random roll a fractional rate needs only
 * when the pool is at the floor.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuHunger
 * @since 1.0.0
 */

final class GuChanneling {

    static final int CHANNEL_MAX_TICKS = 72_000;
    static final int STEP_TICKS = 2;
    static final int ESSENCE_FLOOR = 20;
    private static final int STEPS_PER_ROUND = 8;
    private static final int ROUND_TICKS = STEPS_PER_ROUND * STEP_TICKS;

    private final int essencePerRound;
    private final GuHunger hunger;

    GuChanneling(GuProperties guProperties, GuHunger hunger) { this(guProperties.getEssencePerRound(), hunger); }

    GuChanneling(int essencePerRound, GuHunger hunger) {
        this.essencePerRound = essencePerRound;
        this.hunger = hunger;
    }

    //region sizing a step [定量]
    int stepAmount(ServerPlayer player, ItemStack stack, int elapsed, boolean guPaced) {
        return stepAmount(getStepPool(player), guPaced, elapsed, TendedGuItem.getState(stack).investedEssence(),
                getEssenceAboveHungerFloor(player, stack), TimeFlow.getRate(player), player.getRandom());
    }

    int stepAmount(long pool, boolean guPaced, int elapsed, int invested, long aboveHungerFloor, double rate,
                   RandomSource random) {
        if (pool < ESSENCE_FLOOR) return 0;

        int leftInThisRound = essencePerRound - invested % essencePerRound;
        long thisStep = Math.min(getMostThisStepMaySpend(pool, guPaced, elapsed, rate, random), aboveHungerFloor);

        return (int) Math.min(thisStep, leftInThisRound);
    }

    long getEssenceAboveHungerFloor(Player player, ItemStack stack) {
        return getEssenceAboveHungerFloor(player.getAbilities().instabuild, hunger.getEssenceAboveHungerFloor(stack));
    }

    static long getEssenceAboveHungerFloor(boolean creative, long aboveHungerFloor) {
        return creative ? Long.MAX_VALUE : aboveHungerFloor;
    }

    long getMostThisStepMaySpend(long pool, boolean guPaced, int elapsed, double rate, RandomSource random) {
        if (guPaced) {
            return Math.min(pool, TimeFlow.scale(rate, Math.ceilDiv((long) essencePerRound, STEPS_PER_ROUND), random));
        }
        return getPoolPacedStep(pool, elapsed, rate, random);
    }

    long getPoured(ServerPlayer player, int elapsed, boolean guPaced) {
        return getPoured(getStepPool(player), guPaced, elapsed, TimeFlow.getRate(player), player.getRandom());
    }

    long getPoured(long pool, boolean guPaced, int elapsed, double rate, RandomSource random) {
        if (pool < ESSENCE_FLOOR) return 0L;
        return guPaced ? pool : getPoolPacedStep(pool, elapsed, rate, random);
    }

    private static long getStepPool(ServerPlayer player) {
        boolean creative = player.getAbilities().instabuild;
        return getStepPool(creative ? PrimevalStoneSupply.getPoolAfterTopUp(player)
                : ApertureEssenceService.getSpendable(player), creative);
    }

    static long getStepPool(long spendable, boolean creative) {
        return creative ? Math.max(spendable, ESSENCE_FLOOR) : spendable;
    }

    static long getPoolPacedStep(long pool, int elapsed, double rate, RandomSource random) {
        int stepIndex = (elapsed % ROUND_TICKS) / STEP_TICKS;
        return Math.min(pool, TimeFlow.scale(rate, pool / (STEPS_PER_ROUND + 1 - stepIndex), random));
    }
    //endregion

    //region pouring [灌注入蛊]
    void pour(TendedGuItem gu, ServerPlayer player, ItemStack stack, int amount) {
        int pouredBefore = TendedGuItem.getState(stack).investedEssence();
        int pouredAfter = pouredBefore + amount;

        hunger.billHungerForEssence(stack, pouredBefore, pouredAfter);
        gu.store(stack, TendedGuItem.getState(stack).withInvested(pouredAfter));

        if (pouredAfter >= essencePerRound) {
            gu.applyEffectWithCooldown(player, stack);
            gu.store(stack, TendedGuItem.getState(stack).withInvested(0));
            if (gu.checkEffect(player, stack) != null) player.stopUsingItem();
        }
    }
    //endregion
}
