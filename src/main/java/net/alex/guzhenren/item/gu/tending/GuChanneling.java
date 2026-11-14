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
 * 持续照顾型蛊[Tended Gu]的灌注[Channel]定额：灌注一轮或炼化野生蛊时每步可倒的真元[Essence]。
 * <ul>
 *   <li>宿主 {@code TendedGuItem} 管钩子、进食、付费，本类只定量、记倒入量，不倒过一轮终点；灌注步以饱食[Hunger]下限之
 *       上的真元封顶，故宿主先进食再定量。</li>
 *   <li>⚠ 炼化、灌注各每 {@link #STEP_TICKS}（2 tick）一步。持有者转数高于蛊时按蛊定速：灌注每步至多一轮
 *       1/{@link #STEPS_PER_ROUND}，每 {@link #ROUND_TICKS}（8 步，16 tick = 0.8 秒）一轮，经 {@code TimeFlow} 缩放、
 *       不看饱食；炼化每步可倒整池。否则由池定速：8 步一周期，每步依次倒池的 1/9、1/8……1/2，常速一周期共 8/9。</li>
 *   <li>⚠ 池低于 {@link #ESSENCE_FLOOR} 则每步为 0；创造模式不扣元石[Primeval Stone]，补足抬不高池，故按补足后应剩的池
 *       （{@code PrimevalStoneSupply.getPoolAfterTopUp}）定量，不低于下限，忽略饱食下限。</li>
 *   <li>☠ 若按裸池，创造空池每步仅下限 1/9 即 2，而三转炼化要 51,200。</li>
 *   <li>定量方法皆有纯数值重载供测试（同 {@code TimeFlow} 算术）；玩家重载读数调用，池在下限才耗分数倍率的随机数。</li>
 * </ul>
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
