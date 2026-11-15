package net.alex.guzhenren.entity.projectile;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoonBladeArcTest {

    private static final Vec3 START = new Vec3(0.0D, 2.0D, 0.0D);
    private static final Vec3 END = new Vec3(0.0D, 2.0D, 12.0D);
    private static final float SPEED = 1.5F;
    private static final double EPSILON = 1.0E-6D;

    @Test
    @DisplayName("the arc starts where it was fired and ends on the target")
    void endpointsAreStartAndTarget() {
        MoonBladeArc arc = MoonBladeArc.of(START, END, true, 0.25F, SPEED);
        assertVec(START, arc.pointAt(END, 0.0D));
        assertVec(END, arc.pointAt(END, 1.0D));
    }

    @Test
    @DisplayName("a left bend swings the midpoint east of a flight south, by the ratio of the chord, and lifts it")
    void leftBendSwingsEastAndLifts() {
        MoonBladeArc arc = MoonBladeArc.of(START, END, true, 0.25F, SPEED);
        Vec3 bend = arc.bend();
        assertEquals(3.0D, bend.x, EPSILON, "0.25 of a 12-block chord to the east");
        assertEquals(3.0D * MoonBladeArc.LIFT_RATIO, bend.y, EPSILON);
        assertEquals(0.0D, bend.z, EPSILON);
        Vec3 middle = arc.pointAt(END, 0.5D);
        assertEquals(1.5D, middle.x, EPSILON, "the curve passes half the bend at its middle");
        assertEquals(6.0D, middle.z, EPSILON);
    }

    @Test
    @DisplayName("a right bend mirrors the left one")
    void rightBendMirrorsLeft() {
        Vec3 left = MoonBladeArc.bend(START, END, true, 0.3F);
        Vec3 right = MoonBladeArc.bend(START, END, false, 0.3F);
        assertEquals(-left.x, right.x, EPSILON);
        assertEquals(left.y, right.y, EPSILON);
    }

    @Test
    @DisplayName("the end is read afresh: a moved target still ends the curve")
    void movedTargetStillEndsTheCurve() {
        MoonBladeArc arc = MoonBladeArc.of(START, END, false, 0.2F, SPEED);
        Vec3 moved = END.add(4.0D, 0.0D, 1.0D);
        assertVec(moved, arc.pointAt(moved, 1.0D));
    }

    @Test
    @DisplayName("the tangent rears and swings out at the start, dives and swings back at the end")
    void tangentRearsThenDives() {
        MoonBladeArc arc = MoonBladeArc.of(START, END, true, 0.25F, SPEED);
        assertVec(new Vec3(6.0D, 1.2D, 12.0D), arc.tangentAt(END, 0.0D));
        assertVec(new Vec3(-6.0D, -1.2D, 12.0D), arc.tangentAt(END, 1.0D));
    }

    @Test
    @DisplayName("the tangent is the derivative of the curve, and a straight arc's tangent is its chord")
    void tangentIsTheDerivative() {
        MoonBladeArc arc = MoonBladeArc.of(START, END, false, 0.3F, SPEED);
        Vec3 moved = END.add(2.0D, 1.0D, -1.0D);
        double step = 1.0E-4D;
        Vec3 slope = arc.pointAt(moved, 0.4D + step).subtract(arc.pointAt(moved, 0.4D - step)).scale(0.5D / step);
        assertVec(slope, arc.tangentAt(moved, 0.4D));
        assertVec(END.subtract(START), new MoonBladeArc(START, Vec3.ZERO, 8).tangentAt(END, 0.7D));
    }

    @Test
    @DisplayName("flight ticks cover the chord at the speed, never under two")
    void flightTicksCoverTheChord() {
        assertEquals(8, MoonBladeArc.flightTicks(12.0D, SPEED));
        assertEquals(9, MoonBladeArc.flightTicks(12.1D, SPEED), "a part tick rounds up");
        assertEquals(2, MoonBladeArc.flightTicks(0.5D, SPEED), "a point-blank shot still takes two ticks");
        assertEquals(8, MoonBladeArc.of(START, END, true, 0.2F, SPEED).flightTicks());
    }

    @Test
    @DisplayName("the bank leans toward the bend, 30 degrees at the largest and in proportion below")
    void bankFollowsTheBend() {
        assertEquals(MoonBladeArc.MAX_BANK_DEGREES,
                MoonBladeArc.of(START, END, true, MoonBladeArc.MAX_BEND_RATIO, SPEED).bankDegrees(END), 1.0E-4F);
        assertEquals(-MoonBladeArc.MAX_BANK_DEGREES,
                MoonBladeArc.of(START, END, false, MoonBladeArc.MAX_BEND_RATIO, SPEED).bankDegrees(END), 1.0E-4F);
        assertEquals(MoonBladeArc.MAX_BANK_DEGREES * MoonBladeArc.MIN_BEND_RATIO / MoonBladeArc.MAX_BEND_RATIO,
                MoonBladeArc.of(START, END, true, MoonBladeArc.MIN_BEND_RATIO, SPEED).bankDegrees(END), 1.0E-4F);
        assertEquals(0.0F, new MoonBladeArc(START, Vec3.ZERO, 8).bankDegrees(END), 0.0F, "no bend, no bank");
    }

    @Test
    @DisplayName("a straight-up chord still bends, along the x axis")
    void verticalChordBendsAlongX() {
        Vec3 bend = MoonBladeArc.bend(START, START.add(0.0D, 10.0D, 0.0D), true, 0.2F);
        assertEquals(2.0D, bend.x, EPSILON);
        assertEquals(0.0D, bend.z, EPSILON);
    }

    @Test
    @DisplayName("a rolled aim stays inside the bend range and picks both sides")
    void rolledAimStaysInRange() {
        RandomSource random = RandomSource.create(7L);
        boolean sawLeft = false;
        boolean sawRight = false;
        for (int i = 0; i < 64; i++) {
            MoonBladeArc arc = MoonBladeArc.aim(START, END, SPEED, random);
            double ratio = Math.abs(arc.bend().x) / 12.0D;
            assertTrue(ratio >= MoonBladeArc.MIN_BEND_RATIO - EPSILON && ratio <= MoonBladeArc.MAX_BEND_RATIO + EPSILON,
                    "ratio " + ratio);
            assertTrue(arc.bend().y > 0.0D, "always lifted");
            sawLeft |= arc.bend().x > 0.0D;
            sawRight |= arc.bend().x < 0.0D;
        }
        assertTrue(sawLeft && sawRight, "both sides come up");
    }

    @Test
    @DisplayName("a target is aimable within range and ahead, not behind or too far")
    void aimableIsAheadAndInRange() {
        Vec3 look = new Vec3(0.0D, 0.0D, 1.0D);
        assertTrue(MoonBladeArc.isAimable(START, look, START.add(5.0D, 0.0D, 20.0D), 26.0D));
        assertFalse(MoonBladeArc.isAimable(START, look, START.add(0.0D, 0.0D, -3.0D), 26.0D), "behind");
        assertFalse(MoonBladeArc.isAimable(START, look, START.add(0.0D, 0.0D, 26.5D), 26.0D), "too far");
        assertTrue(MoonBladeArc.isAimable(START, look, START.add(0.0D, 0.0D, 26.0D), 26.0D), "on the edge");
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }

    @Test
    @DisplayName("left is left of the flight whichever way it points: a flight east bends north to the left")
    void leftFollowsTheFlight() {
        Vec3 east = START.add(12.0D, 0.0D, 0.0D);
        assertVec(new Vec3(0.0D, 0.6D, -3.0D), MoonBladeArc.bend(START, east, true, 0.25F));
        assertVec(new Vec3(0.0D, 0.6D, 3.0D), MoonBladeArc.bend(START, east, false, 0.25F));
        assertEquals(MoonBladeArc.MAX_BANK_DEGREES,
                MoonBladeArc.of(START, east, true, MoonBladeArc.MAX_BEND_RATIO, SPEED).bankDegrees(east), 1.0E-4F);
        assertEquals(-MoonBladeArc.MAX_BANK_DEGREES,
                MoonBladeArc.of(START, east, false, MoonBladeArc.MAX_BEND_RATIO, SPEED).bankDegrees(east), 1.0E-4F);
    }

    @Test
    @DisplayName("a chord with no run, straight up, does not bank however much it bends")
    void aStraightUpChordDoesNotBank() {
        MoonBladeArc arc = new MoonBladeArc(START, new Vec3(2.0D, 0.0D, 0.0D), 8);
        assertEquals(0.0F, arc.bankDegrees(START.add(0.0D, 10.0D, 0.0D)), 0.0F);
    }

    @Test
    @DisplayName("a target level with the eye, square across the look, is not ahead")
    void aTargetSquareAcrossIsNotAimable() {
        assertFalse(MoonBladeArc.isAimable(START, new Vec3(0.0D, 0.0D, 1.0D), START.add(5.0D, 0.0D, 0.0D), 26.0D));
    }
}
