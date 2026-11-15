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
        assertTrue(AreaImpact.isInCone(origin, axis, RANGE, origin.add(axis.scale(4.99D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, origin.add(axis.scale(5.01D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, origin.subtract(axis)), "the cone reached behind its origin");
        assertTrue(AreaImpact.isInCone(origin, axis, RANGE, origin.add(turned(axis, side, 44.0D, 4.0D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, origin.add(turned(axis, side, 46.0D, 4.0D))));
        assertFalse(AreaImpact.isInCone(origin, axis, RANGE, origin.add(EAST.scale(2.0D).add(0.0D, 2.0D, 0.0D))),
                "a point 90 degrees off the axis slipped in");
    }

    @Test
    @DisplayName("a 5-block cone reaches 5 blocks along the axis")
    void coneReachesFiveBlocks() {
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, new Vec3(5.0D, 0.0D, 0.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, new Vec3(5.01D, 0.0D, 0.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, new Vec3(-1.0D, 0.0D, 0.0D)), "the cone reached behind");
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, ORIGIN), "a target at the eyes slipped the cone");
    }

    @Test
    @DisplayName("the cone opens 45 degrees off the axis, up and down as well as sideways")
    void coneOpensFortyFiveDegrees() {
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, atDegrees(44.0D, 4.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, atDegrees(46.0D, 4.0D)));
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, RANGE, new Vec3(3.0D, -2.9D, 0.0D)), "a target below the fist slipped");
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, RANGE, new Vec3(3.0D, 0.0D, 3.1D)));
    }

    @Test
    @DisplayName("the range grows with the root of the points from 3 blocks and stops at 12")
    void rangeGrowsWithTheRootOfThePoints() {
        assertEquals(3.0D, AreaImpact.getRange(0), 1.0E-9D);
        assertEquals(3.0D, AreaImpact.getRange(-5), 1.0E-9D);
        assertEquals(7.0D, AreaImpact.getRange(100), 1.0E-9D);
        assertEquals(11.0D, AreaImpact.getRange(400), 1.0E-9D);
        assertEquals(AreaImpact.MAX_RANGE, AreaImpact.getRange(10_000), 1.0E-9D);
    }

    @Test
    @DisplayName("the cone stops at whatever range it is given")
    void coneStopsAtItsGivenRange() {
        assertTrue(AreaImpact.isInCone(ORIGIN, EAST, 11.0D, new Vec3(10.9D, 0.0D, 0.0D)));
        assertFalse(AreaImpact.isInCone(ORIGIN, EAST, 11.0D, new Vec3(11.1D, 0.0D, 0.0D)));
    }

    @Test
    @DisplayName("each target takes half the attack panel")
    void halfTheAttack() {
        assertEquals(6.0F, AreaImpact.getDamage(12.0D));
        assertEquals(0.5F, AreaImpact.getDamage(1.0D));
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
