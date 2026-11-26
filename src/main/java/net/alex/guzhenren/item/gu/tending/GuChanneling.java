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
 * 持续照顾型蛊[Tended Gu]的灌注[Channel]定额计算器：负责计算单轮灌注或炼化野生蛊时每步允许注入的真元上限。
 * <ul>
 *   <li>宿主 {@link TendedGuItem} 负责交互钩子、进食与实际扣费，本类仅计算单步注入量并推进已投入计数，保证不超出单轮上
 *       限；灌注步会以饱食[Hunger]下限之上的可用点数作为封顶，因此宿主必须先执行进食再调用本类定量；满一轮后为伤势计
 *       次，伤重而亡即停手扣栈，宿主 {@code pour} 后先判空。</li>
 *   <li>⚠ 炼化与灌注均每 {@link #STEP_TICKS}（2 tick）推进一次。当持有者转数高于蛊虫时按蛊虫节奏定速：灌注每步至多注入
 *       一轮的 1/{@link #STEPS_PER_ROUND}，每 {@link #ROUND_TICKS}（8 步，16 tick = 0.8 秒）完成一轮并经
 *       {@link TimeFlow} 缩放且不看饱食；炼化每步至多可注入整池真元。其余情况按真元池比例定速：8 步为一个周期，每步依次
 *       注入当前真元池的 1/9、1/8 直至 1/2，常速下一周期累计注入 8/9。</li>
 *   <li>⚠ 真元池低于 {@link #ESSENCE_FLOOR} 时每步定量为 0；由于创造模式下使用元石[Primeval Stone]不扣物品且补足无法抬
 *       高真元池，定量必须以补足后应余的真元池（{@link PrimevalStoneSupply#getPoolAfterTopUp}）为准并忽略饱食下限；若改
 *       按裸池计算，创造模式空池每步仅能注入下限的 1/9（即 2 点），无法完成需要 51,200 点真元的三转炼化。</li>
 *   <li>所有定量方法均提供纯数值重载供单元测试直接调用（与 {@code TimeFlow} 算术一致），玩家重载仅在读取状态后委托给纯
 *       数值版本；当真元池低于下限时直接返回 0 且不掷随机数，否则仅在 {@code TimeFlow.scale} 产生小数余数时掷一次随
 *       机。</li>
 * </ul>
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
            if (gu.wearUse(player, stack)) {
                player.stopUsingItem();
                stack.shrink(1);
                return;
            }
            if (gu.checkEffect(player, stack) != null) player.stopUsingItem();
        }
    }
    //endregion
}
