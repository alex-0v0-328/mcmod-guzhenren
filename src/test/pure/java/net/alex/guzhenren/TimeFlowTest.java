package net.alex.guzhenren;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TimeFlow} 算术：普通时钟恒等；加速倍率缩短催动阶梯；{@code scale}、{@code getSteps} 随倍率变；分数倍率取整后期
 * 望精确；{@link #RATES} 为两更蛊[Second Watch Gu]、三更蛊[Third Watch Gu]效果可组合的全部倍率。
 */

class TimeFlowTest {

    private static final int[] RATES = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13 };
    private static final int DRAWS = 100_000;

    @Test
    @DisplayName("an ordinary clock changes nothing at all")
    void ordinaryClockIsIdentity() {
        RandomSource random = RandomSource.create(1L);
        assertEquals(20, TimeFlow.shortenWait(TimeFlow.NORMAL_RATE, 20));
        assertEquals(7L, TimeFlow.scale(TimeFlow.NORMAL_RATE, 7L, random));
        assertEquals(1, TimeFlow.getSteps(TimeFlow.NORMAL_RATE, random));
    }

    @Test
    @DisplayName("the use ladder shortens but never reaches zero")
    void theUseLadderShortens() {
        assertEquals(2, TimeFlow.shortenWait(2, MortalGuItem.USE_FAST_TICKS));
        assertEquals(5, TimeFlow.shortenWait(2, MortalGuItem.USE_SAME_TICKS));
        assertEquals(Ticks.HALF_SECOND, TimeFlow.shortenWait(2, MortalGuItem.USE_SLOW_TICKS));

        assertEquals(1, TimeFlow.shortenWait(3, MortalGuItem.USE_FAST_TICKS));
        assertEquals(3, TimeFlow.shortenWait(3, MortalGuItem.USE_SAME_TICKS));
        assertEquals(6, TimeFlow.shortenWait(3, MortalGuItem.USE_SLOW_TICKS));
    }

    @Test
    @DisplayName("a wait shorter than the rate floors at one tick, never at none")
    void aShortWaitNeverBecomesNoWait() {
        for (int rate : RATES) {
            assertEquals(1, TimeFlow.shortenWait(rate, 1), "rate " + rate);
            assertTrue(TimeFlow.shortenWait(rate, GuItem.COOLDOWN_TICKS) >= 1, "rate " + rate);
        }
        assertEquals(0, TimeFlow.shortenWait(3, 0));
    }

    @Test
    @DisplayName("what a step pays out scales with the rate and nothing else")
    void aStepScalesWithTheRate() {
        RandomSource random = RandomSource.create(1L);
        for (int rate : RATES) {
            assertEquals(100L * rate, TimeFlow.scale(rate, 100L, random), "rate " + rate);
            assertEquals(0.5D * rate, TimeFlow.scale(rate, 0.5D), 0.0D, "rate " + rate);
            assertEquals(rate, TimeFlow.getSteps(rate, random), "rate " + rate);
        }
        assertEquals(Long.MAX_VALUE, TimeFlow.scale(2, Long.MAX_VALUE, random));
        assertEquals(Long.MAX_VALUE, TimeFlow.scale(13, Long.MAX_VALUE / 2, random));
    }

    @Test
    @DisplayName("the slowest clock stretches a wait by the floor rate")
    void theSlowestClockStretchesAWait() {
        assertEquals(160, TimeFlow.shortenWait(TimeFlow.MIN_RATE, 20));
        assertEquals(2, TimeFlow.shortenWait(0.5D, 1));
    }

    @Test
    @DisplayName("a fractional rate rounds up by its fraction, so the expected count is exact")
    void aFractionalRateRoundsToAnExactExpectation() {
        RandomSource random = RandomSource.create(1L);
        long scaled = 0L;
        long steps = 0L;
        for (int i = 0; i < DRAWS; i++) {
            long one = TimeFlow.scale(0.5D, 3L, random);
            int step = TimeFlow.getSteps(TimeFlow.MIN_RATE, random);
            assertTrue(one == 1L || one == 2L, "0.5 x 3 rounds to 1 or 2, got " + one);
            assertTrue(step == 0 || step == 1, "0.125 steps round to 0 or 1, got " + step);
            scaled += one;
            steps += step;
        }
        assertEquals(1.5D, (double) scaled / DRAWS, 0.01D);
        assertEquals(TimeFlow.MIN_RATE, (double) steps / DRAWS, 0.005D);
    }
}
