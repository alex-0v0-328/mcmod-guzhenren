package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeastChargeArcTest {

    private static final Vec3 START = new Vec3(0.0D, 2.0D, 0.0D);
    private static final Vec3 END = new Vec3(0.0D, 2.0D, 12.0D);
    private static final float SPEED = 1.5F;
    private static final double EPSILON = 1.0E-6D;

    @Test
    @DisplayName("the turn runs from level left, over the top, to level right of a flight south")
    void turnSweepsTheUpperHalfCircle() {
        assertVec(new Vec3(3.0D, 0.0D, 0.0D), BeastChargeArc.bend(START, END, 0.0F, 0.25F, 0.0F));
        assertVec(new Vec3(0.0D, 3.0D, 0.0D), BeastChargeArc.bend(START, END, 0.5F, 0.25F, 0.0F));
        assertVec(new Vec3(-3.0D, 0.0D, 0.0D), BeastChargeArc.bend(START, END, 1.0F, 0.25F, 0.0F));
    }

    @Test
    @DisplayName("the shift moves the peak along the chord and nowhere else")
    void shiftMovesThePeakAlongTheChord() {
        assertVec(new Vec3(0.0D, 3.0D, 2.4D), BeastChargeArc.bend(START, END, 0.5F, 0.25F, 0.2F));
    }

    @Test
    @DisplayName("no roll bends downward, leaves the bend range or misses the target")
    void rollsStayAboveTheChordAndInRange() {
        RandomSource random = RandomSource.create(7L);
        for (int i = 0; i < 500; i++) {
            MoonBladeArc arc = BeastChargeArc.aim(START, END, SPEED, random, 0, 1);
            Vec3 bend = arc.bend();
            double across = Math.sqrt(bend.x * bend.x + bend.y * bend.y);
            assertTrue(bend.y >= -EPSILON, "bent downward: " + bend);
            assertTrue(across >= 12.0D * BeastChargeArc.MIN_BEND_RATIO - EPSILON, "too flat: " + bend);
            assertTrue(across <= 12.0D * BeastChargeArc.MAX_BEND_RATIO + EPSILON, "too bent: " + bend);
            assertTrue(Math.abs(bend.z) <= 12.0D * BeastChargeArc.MAX_SHIFT_RATIO + EPSILON, "peak too far: " + bend);
            assertVec(END, arc.pointAt(END, 1.0D));
            assertEquals(8, arc.flightTicks());
        }
    }

    @Test
    @DisplayName("the charges of one punch each keep to their own sector, left to right")
    void chargesOfOnePunchKeepToTheirSectors() {
        RandomSource random = RandomSource.create(11L);
        int count = 6;
        for (int round = 0; round < 100; round++) {
            for (int index = 0; index < count; index++) {
                Vec3 bend = BeastChargeArc.aim(START, END, SPEED, random, index, count).bend();
                double turn = Math.atan2(bend.y, bend.x) / Math.PI;
                assertTrue(turn >= index / (double) count - EPSILON && turn <= (index + 1) / (double) count + EPSILON,
                        "charge " + index + " left its sector: " + turn);
            }
        }
    }

    @Test
    @DisplayName("a flight straight up still bends level, not along itself")
    void aVerticalFlightBendsLevel() {
        Vec3 bend = BeastChargeArc.bend(START, START.add(0.0D, 10.0D, 0.0D), 0.5F, 0.3F, 0.0F);
        assertEquals(0.0D, bend.y, EPSILON);
        assertEquals(3.0D, bend.length(), EPSILON);
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }
}
