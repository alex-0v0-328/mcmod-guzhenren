package net.alex.guzhenren.particle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrierRingTrailTest {

    private static final double EPS = 1.0E-6;
    private static final double FULL = 3.27D;
    private static final double SLOW = 1.0D;

    private static final class Flight {

        BarrierRingTrail trail = BarrierRingTrail.start(new Vec3(0.0D, 64.0D, 0.0D));
        double x;
        int tick;
        final List<BarrierRingTrail.Ring> rings = new ArrayList<>();
        final List<Integer> ringTicks = new ArrayList<>();

        Flight fly(double perTick, int ticks) {
            for (int i = 0; i < ticks; i++) {
                x += perTick;
                tick++;
                BarrierRingTrail.Step step = trail.step(new Vec3(x, 64.0D, 0.0D));
                for (BarrierRingTrail.Ring ring : step.rings()) {
                    rings.add(ring);
                    ringTicks.add(tick);
                }
                trail = step.next();
            }
            return this;
        }
    }

    @Test
    @DisplayName("the entry booms alone: no barrier ring for two seconds, however fast")
    void theEntryKeepsTwoSecondsQuiet() {
        assertEquals(0, new Flight().fly(FULL, BarrierRingTrail.QUIET_TICKS).rings.size());
    }

    @Test
    @DisplayName("past the quiet a fast flight forms a ring at once, a block ahead, facing the flight")
    void theFirstRingFormsRightAfterTheQuiet() {
        Flight flight = new Flight().fly(FULL, BarrierRingTrail.QUIET_TICKS + 1);
        assertEquals(1, flight.rings.size());
        assertEquals(BarrierRingTrail.QUIET_TICKS + 1, flight.ringTicks.getFirst());
        BarrierRingTrail.Ring ring = flight.rings.getFirst();
        assertEquals(flight.x - FULL + BarrierRingTrail.HEAD_LEAD, ring.center().x, EPS);
        assertEquals(64.0D, ring.center().y, EPS);
        assertEquals(1.0D, ring.facing().x, EPS);
    }

    @Test
    @DisplayName("then one ring every 16 blocks while the flight stays fast")
    void fastFlightFormsARingEverySixteenBlocks() {
        List<BarrierRingTrail.Ring> rings = new Flight().fly(FULL, 200).rings;
        assertTrue(rings.size() > 10, "rings: " + rings.size());
        for (int i = 1; i < rings.size(); i++) {
            assertEquals(BarrierRingTrail.SPACING, rings.get(i).center().x - rings.get(i - 1).center().x, EPS);
        }
    }

    @Test
    @DisplayName("under three times plain flight nothing forms and no distance counts")
    void slowFlightFormsNothing() {
        Flight flight = new Flight().fly(SLOW, 300);
        assertEquals(0, flight.rings.size());
        flight.fly(FULL, 1);
        assertEquals(1, flight.rings.size(), "the first ring waits for the barrier, then forms at once");
    }

    @Test
    @DisplayName("hovering forms nothing")
    void hoveringFormsNothing() {
        Flight flight = new Flight().fly(FULL, 80);
        int before = flight.rings.size();
        flight.fly(0.0D, 60);
        assertEquals(before, flight.rings.size());
    }

    @Test
    @DisplayName("a teleport lays no rings along the jump and resets the speed")
    void aTeleportLaysNoRings() {
        Flight flight = new Flight().fly(FULL, 80);
        int before = flight.rings.size();
        flight.fly(BarrierRingTrail.TELEPORT_DISTANCE + 50.0D, 1);
        assertEquals(before, flight.rings.size());
        assertEquals(0.0D, flight.trail.speed(), EPS);
    }
}
