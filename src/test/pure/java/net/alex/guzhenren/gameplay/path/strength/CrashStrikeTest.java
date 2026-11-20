package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashStrikeTest {

    private static final double DELTA = 1.0E-2D;
    private static final Vec3 FEET = new Vec3(0.5D, 64.0D, 0.5D);
    private static final double HEIGHT = 1.8D;
    private static final double RADIUS = 2.0D;
    private static final AABB AWAY = new AABB(20.2D, 64.0D, 0.2D, 20.8D, 65.8D, 0.8D);

    @Test
    @DisplayName("the strike starts at half the bare fist and the two boars add a quarter")
    void theBoarsRaiseTheStrike() {
        assertEquals(0.5F, CrashStrike.getDamageRatio(false));
        assertEquals(0.75F, CrashStrike.getDamageRatio(true));
    }

    @Test
    @DisplayName("the dash stops after a block and two ticks in a row under half a block, or at the deadline")
    void theDashStopsWhenItSlowsAfterMoving() {
        assertFalse(CrashStrike.hasStopped(0.0D, 3, 3, 13), "the dash stopped before its first move arrived");
        assertFalse(CrashStrike.hasStopped(6.0D, 0, 1, 13), "the dash stopped mid-flight");
        assertFalse(CrashStrike.hasStopped(6.2D, 1, 3, 13), "one tick without a position stopped the dash");
        assertTrue(CrashStrike.hasStopped(6.2D, 2, 4, 13));
        assertFalse(CrashStrike.hasStopped(0.4D, 9, 12, 13), "a dash against a wall struck before the deadline");
        assertTrue(CrashStrike.hasStopped(0.4D, 10, 13, 13));
    }

    @Test
    @DisplayName("the deadline covers the link, the clip, the capped ticks, three spare ticks and the round trip")
    void theDeadlineCoversTheWholeDash() {
        assertEquals(13, CrashStrike.getDeadline(1.0D, 6.05D, 0), "2 link + 7 clip + 1 capped + 3");
        assertEquals(13, CrashStrike.getDeadline(2.0D, 32.0D, 0), "2 link + 4 clip + 4 capped + 3");
        assertEquals(16, CrashStrike.getDeadline(2.0D, 32.0D, 120), "a 120 ms round trip adds 3 ticks");
        assertEquals(33, CrashStrike.getDeadline(2.0D, 32.0D, 5000), "the round trip counts at most 20 ticks");
    }

    @Test
    @DisplayName("the sources read only the total points, the two boars and the horse: 21.1 and 13.9 blocks")
    void theSourcesReadOnlyPointsBoarsAndHorse() {
        CrashSources beasts = new CrashSources(96, true, 1.0D, 0.5D);
        assertEquals(21.06D, beasts.getTravel(), DELTA);
        assertEquals(1.5D, beasts.getPlaySpeed(), DELTA);
        assertEquals(3.0F, beasts.getMaxHardness());
        assertEquals(6.92D, beasts.getStrikeRange(), DELTA);
        assertEquals(0.75F, beasts.getDamageRatio());

        CrashSources human = new CrashSources(168, false, 0.0D, 0.0D);
        assertEquals(13.90D, human.getTravel(), DELTA);
        assertEquals(1.0D, human.getPlaySpeed(), DELTA);
        assertEquals(0.6F, human.getMaxHardness());
        assertEquals(8.18D, human.getStrikeRange(), DELTA);
        assertEquals(0.5F, human.getDamageRatio());

        CrashSources bare = new CrashSources(0, false, 0.0D, 0.0D);
        assertEquals(6.05D, bare.getTravel(), DELTA);
        assertEquals(AreaDamageShatter.NONE, bare.getMaxHardness());
        assertEquals(3.0D, bare.getStrikeRange(), DELTA);
    }

    @Test
    @DisplayName("the strike radius is the unwidened area damage range of the dash's points")
    void theStrikeRadiusIsNeverWidened() {
        for (int points : new int[] { 0, 48, 112, 400, 10_000 }) {
            CrashSources sources = new CrashSources(points, false, 0.0D, 0.0D);
            assertEquals(AreaDamage.getRange(points), sources.getStrikeRange(), 1.0E-9D);
            assertEquals(AreaDamage.getRange(points, 0), sources.getStrikeRange(), 1.0E-9D);
        }
        assertEquals(12.0D, new CrashSources(10_000, true, 1.0D, 0.5D).getStrikeRange(), 1.0E-9D);
    }

    @Test
    @DisplayName("the end disc circles the stop from the feet layer to above the head, behind too, and spares the floor")
    void theEndDiscSparesTheFloor() {
        List<BlockPos> cells = CrashStrike.getEndCells(FEET, HEIGHT, RADIUS);
        assertTrue(cells.contains(new BlockPos(0, 64, 0)), "the stop's own column was left out");
        assertTrue(cells.contains(new BlockPos(-2, 64, 0)), "the disc skipped the cells behind the stop");
        assertTrue(cells.contains(new BlockPos(2, 66, 0)), "the disc stopped below the layer above the head");
        assertFalse(cells.contains(new BlockPos(0, 63, 0)), "the end disc dug the floor");
        assertFalse(cells.contains(new BlockPos(0, 67, 0)), "the disc reached two layers above the head");
        assertFalse(cells.contains(new BlockPos(2, 64, 2)), "the disc reached past its radius");
    }

    @Test
    @DisplayName("the start pit adds the floor layer and spares the floor the dasher still stands on")
    void theStartPitDigsTheFloor() {
        List<BlockPos> away = CrashStrike.getPitCells(FEET, HEIGHT, RADIUS, AWAY);
        assertTrue(away.contains(new BlockPos(0, 63, 0)), "the pit spared the floor underfoot");
        assertTrue(away.contains(new BlockPos(-2, 63, 0)), "the pit's floor stopped short behind");
        assertTrue(away.contains(new BlockPos(1, 64, 0)), "the pit skipped the feet layer");
        assertFalse(away.contains(new BlockPos(0, 62, 0)), "the pit went two layers down");

        AABB standing = new AABB(1.2D, 64.0D, 0.2D, 1.8D, 65.8D, 0.8D);
        List<BlockPos> under = CrashStrike.getPitCells(FEET, HEIGHT, RADIUS, standing);
        assertFalse(under.contains(new BlockPos(1, 63, 0)), "the pit dug the floor the dasher stands on");
        assertTrue(under.contains(new BlockPos(1, 64, 0)), "standing there spared the feet layer too");
        assertTrue(under.contains(new BlockPos(0, 63, 0)), "standing beside it spared the next column's floor");
    }

    @Test
    @DisplayName("the pit waits until the dasher is past its radius plus 1.2 blocks, measured flat")
    void thePitWaitsForTheDasherToLeave() {
        assertFalse(CrashStrike.hasLeftPit(FEET, FEET.add(3.2D, 0.0D, 0.0D), RADIUS), "the pit opened under the edge");
        assertTrue(CrashStrike.hasLeftPit(FEET, FEET.add(3.3D, 0.0D, 0.0D), RADIUS));
        assertFalse(CrashStrike.hasLeftPit(FEET, FEET.add(0.0D, 5.0D, 0.0D), RADIUS), "height counted toward leaving");
    }
}
