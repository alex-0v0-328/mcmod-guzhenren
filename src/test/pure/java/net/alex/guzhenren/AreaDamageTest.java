package net.alex.guzhenren;

import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaDamageTest {

    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final double RANGE = 5.0D;
    private static final double NARROW = 45.0D;
    private static final double WIDEST = 72.5D;
    private static final double DELTA = 1.0E-9D;

    @Test
    @DisplayName("a cone from any origin follows its own axis and opens 45 degrees")
    void coneFollowsAnyOriginAndAxis() {
        Vec3 origin = new Vec3(10.0D, 64.0D, -3.0D);
        Vec3 axis = new Vec3(1.0D, -1.0D, 0.0D).normalize();
        Vec3 side = new Vec3(0.0D, 0.0D, 1.0D);
        assertTrue(AreaDamage.isInCone(origin, axis, RANGE, NARROW, origin.add(axis.scale(4.99D))));
        assertFalse(AreaDamage.isInCone(origin, axis, RANGE, NARROW, origin.add(axis.scale(5.01D))));
        assertFalse(AreaDamage.isInCone(origin, axis, RANGE, NARROW, origin.subtract(axis)),
                "the cone reached behind its origin");
        assertTrue(AreaDamage.isInCone(origin, axis, RANGE, NARROW, origin.add(turned(axis, side, 44.0D, 4.0D))));
        assertFalse(AreaDamage.isInCone(origin, axis, RANGE, NARROW, origin.add(turned(axis, side, 46.0D, 4.0D))));
        assertFalse(AreaDamage.isInCone(origin, axis, RANGE, NARROW,
                origin.add(EAST.scale(2.0D).add(0.0D, 2.0D, 0.0D))), "a point 90 degrees off the axis slipped in");
    }

    @Test
    @DisplayName("a 5-block cone reaches 5 blocks along the axis")
    void coneReachesFiveBlocks() {
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(5.0D, 0.0D, 0.0D)));
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(5.01D, 0.0D, 0.0D)));
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(-1.0D, 0.0D, 0.0D)),
                "the cone reached behind");
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, ORIGIN), "a target at the eyes slipped the cone");
    }

    @Test
    @DisplayName("tier 0 opens 45 degrees off the axis, up and down as well as sideways")
    void coneOpensFortyFiveDegrees() {
        assertEquals(NARROW, AreaDamage.getHalfAngle(0), DELTA);
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, atDegrees(44.0D, 4.0D)));
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, atDegrees(46.0D, 4.0D)));
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(3.0D, -2.9D, 0.0D)),
                "a target below the fist slipped");
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, NARROW, new Vec3(3.0D, 0.0D, 3.1D)));
    }

    @Test
    @DisplayName("each Hardship tier adds 5.5 degrees: 45 / 50.5 / 56 / 61.5 / 67 / 72.5, a 145 degree fan at five")
    void theHalfAngleWidensByTier() {
        double[] expected = { 45.0D, 50.5D, 56.0D, 61.5D, 67.0D, 72.5D };
        for (int tier = 0; tier < expected.length; tier++) {
            assertEquals(expected[tier], AreaDamage.getHalfAngle(tier), DELTA, "tier " + tier);
        }
        assertEquals(145.0D, 2.0D * AreaDamage.getHalfAngle(5), DELTA);
    }

    @Test
    @DisplayName("the tier five fan takes 72 degrees off the axis and never 74, nor anything behind")
    void theWidestFanStopsAtSeventyTwoAndAHalf() {
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, RANGE, WIDEST, atDegrees(72.0D, 4.0D)));
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, RANGE, WIDEST, atDegrees(-72.0D, 4.0D)));
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, WIDEST, atDegrees(74.0D, 4.0D)));
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, WIDEST, atDegrees(90.0D, 4.0D)),
                "the widest fan reached the side");
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, RANGE, WIDEST, atDegrees(180.0D, 4.0D)),
                "the widest fan reached straight behind");
    }

    @Test
    @DisplayName("the range grows with the root of the points from 3 blocks and stops at 12")
    void rangeGrowsWithTheRootOfThePoints() {
        assertEquals(3.0D, AreaDamage.getRange(0), DELTA);
        assertEquals(3.0D, AreaDamage.getRange(-5), DELTA);
        assertEquals(7.0D, AreaDamage.getRange(100), DELTA);
        assertEquals(11.0D, AreaDamage.getRange(400), DELTA);
        assertEquals(12.0D, AreaDamage.getRange(10_000), DELTA);
    }

    @Test
    @DisplayName("each Hardship tier adds 0.4 to the range factor: 1 / 1.4 / 1.8 / 2.2 / 2.6 / 3")
    void theRangeFactorGrowsByTier() {
        double[] expected = { 1.0D, 1.4D, 1.8D, 2.2D, 2.6D, 3.0D };
        for (int tier = 0; tier < expected.length; tier++) {
            assertEquals(expected[tier], AreaDamage.getRangeFactor(tier), DELTA, "tier " + tier);
        }
    }

    @Test
    @DisplayName("the widened range multiplies the range by the tier factor and stops at 12 blocks")
    void theWidenedRangeStopsAtTwelve() {
        assertEquals(3.0D, AreaDamage.getRange(0, 0), DELTA);
        assertEquals(7.0D, AreaDamage.getRange(100, 0), DELTA);
        assertEquals(4.2D, AreaDamage.getRange(0, 1), DELTA);
        assertEquals(9.8D, AreaDamage.getRange(100, 1), DELTA);
        assertEquals(9.0D, AreaDamage.getRange(0, 5), DELTA);
        assertEquals(12.0D, AreaDamage.getRange(100, 5), DELTA, "7 x 3 passed 12");
        assertEquals(12.0D, AreaDamage.getRange(400, 1), DELTA, "11 x 1.4 passed 12");
        assertEquals(12.0D, AreaDamage.getRange(10_000, 0), DELTA);
    }

    @Test
    @DisplayName("the cone stops at whatever range it is given")
    void coneStopsAtItsGivenRange() {
        assertTrue(AreaDamage.isInCone(ORIGIN, EAST, 11.0D, NARROW, new Vec3(10.9D, 0.0D, 0.0D)));
        assertFalse(AreaDamage.isInCone(ORIGIN, EAST, 11.0D, NARROW, new Vec3(11.1D, 0.0D, 0.0D)));
    }

    @Test
    @DisplayName("each target landed takes its health before the hit off the damage left; none is left at 0 or under")
    void theSpillLosesTheHealthOfEachTarget() {
        assertEquals(8.0F, AreaDamage.getSpill(12.0F, 4.0F));
        assertEquals(0.0F, AreaDamage.getSpill(12.0F, 12.0F));
        assertEquals(-8.0F, AreaDamage.getSpill(12.0F, 20.0F));
        assertEquals(1.5F, AreaDamage.getSpill(AreaDamage.getSpill(12.0F, 4.0F), 6.5F));
        assertEquals(8, AreaDamage.MAX_TARGETS);
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
