package net.alex.guzhenren;

import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService.Outcome;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApertureNourishTest {

    @Test
    @DisplayName("冲刷窍壁 ordinary table splits at 40 / 70 / 90")
    void ordinaryBoundaries() {
        assertSame(Outcome.SUCCESS, ApertureNourishService.resolve(0, false));
        assertSame(Outcome.SUCCESS, ApertureNourishService.resolve(39, false));
        assertSame(Outcome.HOLD, ApertureNourishService.resolve(40, false));
        assertSame(Outcome.HOLD, ApertureNourishService.resolve(69, false));
        assertSame(Outcome.DROP_STAGE, ApertureNourishService.resolve(70, false));
        assertSame(Outcome.DROP_STAGE, ApertureNourishService.resolve(89, false));
        assertSame(Outcome.DROP_BASE, ApertureNourishService.resolve(90, false));
        assertSame(Outcome.DROP_BASE, ApertureNourishService.resolve(99, false));
    }

    @Test
    @DisplayName("十绝体 table splits at 60 / 85 -- the two tables cut at different points")
    void extremeBoundaries() {
        assertSame(Outcome.SUCCESS, ApertureNourishService.resolve(0, true));
        assertSame(Outcome.SUCCESS, ApertureNourishService.resolve(59, true));
        assertSame(Outcome.HOLD, ApertureNourishService.resolve(60, true));
        assertSame(Outcome.HOLD, ApertureNourishService.resolve(84, true));
        assertSame(Outcome.DROP_STAGE, ApertureNourishService.resolve(85, true));
        assertSame(Outcome.DROP_STAGE, ApertureNourishService.resolve(99, true));
    }

    @Test
    @DisplayName("十绝体 can never roll the aptitude loss")
    void extremeNeverLosesBase() {
        for (int roll = 0; roll < 100; roll++) {
            assertNotSame(Outcome.DROP_BASE, ApertureNourishService.resolve(roll, true), "roll " + roll);
        }
    }

    @Test
    @DisplayName("every roll in 0 to 99 resolves, and the two tables agree on nothing but their shape")
    void everyRollResolves() {
        int ordinarySuccess = 0;
        int extremeSuccess = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (ApertureNourishService.resolve(roll, false) == Outcome.SUCCESS) ordinarySuccess++;
            if (ApertureNourishService.resolve(roll, true) == Outcome.SUCCESS) extremeSuccess++;
        }
        assertEquals(40, ordinarySuccess);
        assertEquals(60, extremeSuccess);
    }

    @Test
    @DisplayName("冲刷窍壁 costs one and a half Ten-Extremes peak pools -- the ×10 rank ladder, exactly")
    void impactCostLadder() {
        assertEquals(1_200L, cost(Rank.ONE));
        assertEquals(12_000L, cost(Rank.TWO));
        assertEquals(120_000L, cost(Rank.THREE));
        assertEquals(1_200_000L, cost(Rank.FOUR));
    }

    @Test
    @DisplayName("no pool can hold one strike -- even a 十绝 peak pool is only two thirds of it")
    void noPoolCoversAStrike() {
        for (Rank rank : new Rank[] { Rank.ONE, Rank.TWO, Rank.THREE, Rank.FOUR }) {
            long peak = Aperture.computeMaxEssence(rank, Stage.PEAK, Aperture.MAX_BASE);
            assertEquals(cost(rank) * 2L, peak * 3L, "rank " + rank);
        }
    }

    private static long cost(Rank rank) {
        return ApertureNourishService.IMPACT_COST_PER_RANK_BASE * rank.getRankBase();
    }

    @Test
    @DisplayName("the starved anchor defaults to the sentinel, never to zero, and the target defaults to PRIMARY")
    void recordInvariants() {
        assertEquals(ApertureNourishData.NOT_STARVED, ApertureNourishData.DEFAULT.starvedSinceTick());
        assertFalse(ApertureNourishData.DEFAULT.isStarving());
        assertEquals(ApertureData.PRIMARY, ApertureNourishData.DEFAULT.target());
    }

    @Test
    @DisplayName("石窍蛊 lock: petrified and the progress live on the aperture, clamped by its ctor")
    void petrifiedInvariants() {
        Aperture fresh = Aperture.createSecond(Rank.THREE);
        assertFalse(fresh.petrified());
        assertEquals(0, fresh.nourishProgress());
        assertEquals(0, new Aperture(Rank.ONE, Stage.INITIAL, 80, 0L, null, null, 0L, 0, 0L,
                -5, false, false, false).nourishProgress());
        assertEquals(ApertureNourishData.FULL, new Aperture(Rank.ONE, Stage.INITIAL, 80, 0L, null, null,
                0L, 0, 0L, 500, true, false, false).nourishProgress());
        assertTrue(new Aperture(Rank.ONE, Stage.INITIAL, 80, 0L, null, null,
                0L, 0, 0L, 0, true, false, false).petrified());
    }

    @Test
    @DisplayName("a fresh world is not already starved out -- zero is a real game time")
    void freshWorldIsNotStarved() {
        assertFalse(ApertureNourishData.DEFAULT.hasStarvedOut(0L));
        assertFalse(ApertureNourishData.DEFAULT.hasStarvedOut(Long.MAX_VALUE / 2));
    }

    @Test
    @DisplayName("the starve grace runs out only after the full window")
    void starveWindow() {
        ApertureNourishData starving = ApertureNourishData.DEFAULT.withStarvedSinceTick(1_000L);
        assertFalse(starving.hasStarvedOut(1_000L + ApertureNourishData.STARVE_GRACE_TICKS - 1L));
        assertTrue(starving.hasStarvedOut(1_000L + ApertureNourishData.STARVE_GRACE_TICKS));
    }
}
