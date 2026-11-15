package net.alex.guzhenren;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 衰老与寿元计费：一段世界时间在普通、加速宙道时间倍率下折合的年龄、寿元份数；{@link #RATES} 为可达倍率：不戴、
 * 两更蛊[Second Watch Gu]、三更蛊[Third Watch Gu]、同戴；{@link #lived} 与其他花费同一入口。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class AgingDisplayTest {

    private static final int[] RATES = { 1, 2, 3, 5 };
    private static final long BEAT = Ticks.SECOND;

    private static long lived(long elapsedTicks, int rate) {
        return TimeFlow.scale(rate, BodyService.getElapsedParts(elapsedTicks), RandomSource.create());
    }

    @Test
    @DisplayName("a game day is exactly one year, and a heartbeat is a whole number of parts")
    void aDayIsOneYear() {
        assertEquals(BodyData.PARTS_PER_YEAR, Ticks.DAY * BodyData.PARTS_PER_TICK);
        assertEquals(120L, BodyService.getElapsedParts(BEAT));
    }

    @Test
    @DisplayName("☠ a hastened clock spends life FASTER -- the rate multiplies, it never divides")
    void hasteSpendsLifeFaster() {
        long ordinary = lived(BEAT, 1);
        assertEquals(120L, ordinary);

        for (int rate : RATES) {
            assertEquals(ordinary * rate, lived(BEAT, rate), "rate " + rate);
            if (rate > 1) assertTrue(lived(BEAT, rate) > ordinary, "rate " + rate);
        }
    }

    @Test
    @DisplayName("a whole game day costs exactly one year at ordinary speed, and N years at rate N")
    void aDayCostsAYearPerRate() {
        long beatsPerDay = Ticks.DAY / BEAT;
        for (int rate : RATES) {
            assertEquals(BodyData.PARTS_PER_YEAR * rate, lived(BEAT, rate) * beatsPerDay, "rate " + rate);
        }
    }

    @Test
    @DisplayName("one five-minute form costs a quarter of a year per point of rate")
    void oneFormCostsAQuarterYearPerRate() {
        long form = 5L * Ticks.MINUTE;
        assertEquals(BodyData.PARTS_PER_YEAR / 2L, lived(form, 2));
        assertEquals(BodyData.PARTS_PER_YEAR * 3L / 4L, lived(form, 3));
        assertEquals(BodyData.PARTS_PER_YEAR * 5L / 4L, lived(form, 5));
    }

    @Test
    @DisplayName("one long offline stretch bills the same as the beats it stood in for")
    void offlineMatchesBeatByBeat() {
        long beats = 137L;
        for (int rate : RATES) {
            assertEquals(lived(BEAT, rate) * beats, lived(beats * BEAT, rate), "rate " + rate);
        }
    }

    @Test
    @DisplayName("time running backwards bills nothing rather than handing life back")
    void backwardsTimeBillsNothing() {
        for (int rate : RATES) {
            assertEquals(0L, lived(-Ticks.DAY, rate), "rate " + rate);
            assertEquals(0L, lived(0L, rate), "rate " + rate);
        }
    }

    @Test
    @DisplayName("a default body starts on the stated years, not on a bare part count")
    void defaultsAreStatedInYears() {
        assertEquals(BodyData.DEFAULT_AGE, (long) BodyData.DEFAULT.ageYears());
        assertEquals(BodyData.DEFAULT_LIFESPAN, (long) BodyData.DEFAULT.getLifespanYears());
        assertTrue(BodyData.DEFAULT.lifespanParts() > 0L);
    }
}
