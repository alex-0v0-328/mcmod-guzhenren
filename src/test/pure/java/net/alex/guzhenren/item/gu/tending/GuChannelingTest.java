package net.alex.guzhenren.item.gu.tending;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GuChanneling} 定量算术，倍率 1 与 2：真元池节奏每步取池 1/9 至 1/2，一周期付 8/9；蛊节奏每步一轮的 1/8；下限、
 * 轮上限、饱食[Hunger]上限；创造模式真元池。
 */

class GuChannelingTest {

    private static final double NORMAL = 1.0;
    private static final int ROUND = 40;
    private static final int STEP = GuChanneling.STEP_TICKS;
    private final GuChanneling channeling = new GuChanneling(ROUND, new GuHunger.NoClock());
    private final RandomSource random = RandomSource.create(1L);

    @Test
    void poolPacedStepsRunOneNinthUpToOneHalfOfThePool() {
        long[] expected = { 10, 11, 12, 15, 18, 22, 30, 45 };
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], GuChanneling.getPoolPacedStep(90, index * STEP, NORMAL, random));
        }
    }

    @Test
    void poolPacedCycleRepeatsEveryRound() {
        int round = 8 * STEP;
        assertEquals(10, GuChanneling.getPoolPacedStep(90, round, NORMAL, random));
        assertEquals(45, GuChanneling.getPoolPacedStep(90, round + 7 * STEP, NORMAL, random));
    }

    @Test
    void poolPacedCyclePaysEightNinthsOfThePool() {
        long pool = 9_000;
        for (int index = 0; index < 8; index++) {
            pool -= GuChanneling.getPoolPacedStep(pool, index * STEP, NORMAL, random);
        }
        assertEquals(1_000, pool);
    }

    @Test
    void aFasterClockScalesTheStepButNeverPastThePool() {
        assertEquals(20, GuChanneling.getPoolPacedStep(90, 0, 2.0, random));
        assertEquals(90, GuChanneling.getPoolPacedStep(90, 7 * STEP, 3.0, random));
    }

    @Test
    void guPacedStepIsAnEighthOfARoundRoundedUp() {
        assertEquals(5, channeling.getMostThisStepMaySpend(1_000, true, 0, NORMAL, random));
        assertEquals(10, channeling.getMostThisStepMaySpend(1_000, true, 0, 2.0, random));
        assertEquals(6, new GuChanneling(41, new GuHunger.NoClock())
                .getMostThisStepMaySpend(1_000, true, 0, NORMAL, random));
    }

    @Test
    void guPacedStepIsCappedByThePool() {
        assertEquals(3, channeling.getMostThisStepMaySpend(3, true, 0, NORMAL, random));
    }

    @Test
    void aPoolBelowTheFloorSizesNothing() {
        int belowFloor = GuChanneling.ESSENCE_FLOOR - 1;
        assertEquals(0, channeling.stepAmount(belowFloor, true, 0, 0, Long.MAX_VALUE, NORMAL, random));
        assertEquals(0L, channeling.getPoured(belowFloor, true, 0, NORMAL, random));
        assertEquals(0L, channeling.getPoured(belowFloor, false, 0, NORMAL, random));
    }

    @Test
    void refiningPoursTheWholePoolWhenGuPacedAndTheCycleStepOtherwise() {
        assertEquals(500L, channeling.getPoured(500, true, 0, NORMAL, random));
        assertEquals(55L, channeling.getPoured(500, false, 0, NORMAL, random));
    }

    @Test
    void aStepNeverPoursPastTheEndOfTheRound() {
        assertEquals(5, channeling.stepAmount(1_000, true, 0, 0, Long.MAX_VALUE, NORMAL, random));
        assertEquals(2, channeling.stepAmount(1_000, true, 0, 38, Long.MAX_VALUE, NORMAL, random));
        assertEquals(35, channeling.stepAmount(1_000, false, 7 * STEP, 5, Long.MAX_VALUE, NORMAL, random));
    }

    @Test
    void aFullRoundOpensAFreshOne() {
        assertEquals(5, channeling.stepAmount(1_000, true, 0, ROUND, Long.MAX_VALUE, NORMAL, random));
    }

    @Test
    void aStepIsCappedByTheEssenceAboveTheHungerFloor() {
        assertEquals(3, channeling.stepAmount(1_000, true, 0, 0, 3, NORMAL, random));
        assertEquals(0, channeling.stepAmount(1_000, true, 0, 0, 0, NORMAL, random));
    }

    @Test
    void creativeIgnoresTheHungerFloorAndSizesFromAtLeastTheFloor() {
        assertEquals(Long.MAX_VALUE, GuChanneling.getEssenceAboveHungerFloor(true, 0));
        assertEquals(7L, GuChanneling.getEssenceAboveHungerFloor(false, 7));
        assertEquals(GuChanneling.ESSENCE_FLOOR, GuChanneling.getStepPool(0, true));
        assertEquals(500, GuChanneling.getStepPool(500, true));
        assertEquals(0, GuChanneling.getStepPool(0, false));
    }
}
