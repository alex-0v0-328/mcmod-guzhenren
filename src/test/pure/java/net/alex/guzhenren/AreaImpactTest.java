package net.alex.guzhenren;

import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaImpactTest {

    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final double RANGE = 5.0D;
    private static final double NARROW = 45.0D;
    private static final double WIDE = 135.0D;

    @Test
    @DisplayName("the chance is 40% under All-Out, 50% with Strength Qi beside it, 10% for Strength Qi alone, 0 bare")
    void chanceFollowsTheRunningEffects() {
        assertEquals(0.4F, AreaImpact.getChance(true, false));
        assertEquals(0.5F, AreaImpact.getChance(true, true));
        assertEquals(0.1F, AreaImpact.getChance(false, true));
        assertEquals(0.0F, AreaImpact.getChance(false, false));
    }

    @Test
    @DisplayName("a cone from any origin follows its own axis and opens 45 degrees")
    void coneFollowsAnyOriginAndAxis() {
        Vec3 origin = new Vec3(10.0D, 64.0D, -3.0D);
        Vec3 axis = new Vec3(1.0D, -1.0D, 0.0D).normalize();
        Vec3 side = new Vec3(0.0D, 0.0D, 1.0D);
        assertTrue(AreaImpact.isInCone(origin, axis, RANGE, NARROW, origin.add(axis.scale(4.99D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, NARROW, origin.add(axis.scale(5.01D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, NARROW, origin.subtract(axis)),
                "the cone reached behind its origin");
        assertTrue(AreaImpact.isInCone(origin, axis, RANGE, NARROW, origin.add(turned(axis, side, 44.0D, 4.0D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, NARROW, origin.add(turned(axis, side, 46.0D, 4.0D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, NARROW,
                origin.add(EAST.scale(2.0D).add(0.0D, 2.0D, 0.0D))), "a point 90 degrees off the axis slipped in");
    }

    @Test
    @DisplayName("a 5-block cone reaches 5 blocks along the axis")
    void coneReachesFiveBlocks() {
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(5.0D, 0.0D, 0.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(5.01D, 0.0D, 0.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(-1.0D, 0.0D, 0.0D)),
                "the cone reached behind");
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, ORIGIN), "a target at the eyes slipped the cone");
    }

    @Test
    @DisplayName("the cone opens 45 degrees off the axis, up and down as well as sideways")
    void coneOpensFortyFiveDegrees() {
        assertEquals(NARROW, AreaImpact.getHalfAngle(false));
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, atDegrees(44.0D, 4.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, atDegrees(46.0D, 4.0D)));
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(3.0D, -2.9D, 0.0D)),
                "a target below the fist slipped");
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(3.0D, 0.0D, 3.1D)));
    }

    @Test
    @DisplayName("the widened cone opens 135 degrees: the sides and the rear quarters, never straight behind")
    void theWidenedConeOpensOneHundredThirtyFiveDegrees() {
        assertEquals(WIDE, AreaImpact.getHalfAngle(true));
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, WIDE, atDegrees(90.0D, 4.0D)), "the side slipped");
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, WIDE, atDegrees(134.0D, 4.0D)));
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, WIDE, atDegrees(-134.0D, 4.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, WIDE, atDegrees(136.0D, 4.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, WIDE, atDegrees(180.0D, 4.0D)),
                "the widened cone reached straight behind");
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, WIDE, atDegrees(90.0D, 5.01D)),
                "the widened cone reached past its range");
    }

    @Test
    @DisplayName("the range grows with the root of the points from 3 blocks and stops at 12")
    void rangeGrowsWithTheRootOfThePoints() {
        assertEquals(3.0D, AreaImpact.getRange(0), 1.0E-9D);
        assertEquals(3.0D, AreaImpact.getRange(-5), 1.0E-9D);
        assertEquals(7.0D, AreaImpact.getRange(100), 1.0E-9D);
        assertEquals(11.0D, AreaImpact.getRange(400), 1.0E-9D);
        assertEquals(12.0D, AreaImpact.getRange(10_000), 1.0E-9D);
    }

    @Test
    @DisplayName("the widened range is one and a half times the range, 18 blocks at most; unwidened it is unchanged")
    void theWidenedRangeIsOneAndAHalfTimes() {
        assertEquals(4.5D, AreaImpact.getRange(0, true), 1.0E-9D);
        assertEquals(10.5D, AreaImpact.getRange(100, true), 1.0E-9D);
        assertEquals(16.5D, AreaImpact.getRange(400, true), 1.0E-9D);
        assertEquals(18.0D, AreaImpact.getRange(10_000, true), 1.0E-9D);
        assertEquals(7.0D, AreaImpact.getRange(100, false), 1.0E-9D);
        assertEquals(12.0D, AreaImpact.getRange(10_000, false), 1.0E-9D);
    }

    @Test
    @DisplayName("the cone stops at whatever range it is given")
    void coneStopsAtItsGivenRange() {
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, 11.0D, NARROW, new Vec3(10.9D, 0.0D, 0.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, 11.0D, NARROW, new Vec3(11.1D, 0.0D, 0.0D)));
    }

    @Test
    @DisplayName("each target landed takes its health before the hit off the damage left; none is left at 0 or under")
    void theSpillLosesTheHealthOfEachTarget() {
        assertEquals(8.0F, AreaImpact.getSpill(12.0F, 4.0F));
        assertEquals(0.0F, AreaImpact.getSpill(12.0F, 12.0F));
        assertEquals(-8.0F, AreaImpact.getSpill(12.0F, 20.0F));
        assertEquals(1.5F, AreaImpact.getSpill(AreaImpact.getSpill(12.0F, 4.0F), 6.5F));
        assertEquals(8, AreaImpact.MAX_TARGETS);
    }

    private static Vec3 atDegrees(double degrees, double distance) {
        double radians = Math.toRadians(degrees);
        return new Vec3(Math.cos(radians) * distance, 0.0D, Math.sin(radians) * distance);
    }

    private static Vec3 turned(Vec3 axis, Vec3 side, double degrees, double distance) {
        double radians = Math.toRadians(degrees);
        return axis.scale(Math.cos(radians) * distance).add(side.scale(Math.sin(radians) * distance));
    }
}
