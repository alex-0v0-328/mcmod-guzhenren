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

class CrashBreachTest {

    private static final AABB BODY = new AABB(4.2D, 1.0D, 0.2D, 4.8D, 2.8D, 0.8D);
    private static final double DELTA = 1.0E-9D;

    @Test
    @DisplayName("the body's box sweeps the level way a quarter block at a time, up to the length")
    void theBoxSweepsTheLevelWay() {
        List<AABB> boxes = CrashBreach.getBoxes(BODY, new Vec3(0.0D, 5.0D, 2.0D), 2.1D);
        assertEquals(10, boxes.size());
        assertEquals(BODY.minZ + 2.1D, boxes.getLast().minZ, 1.0E-2D);
        assertEquals(boxes.getFirst().minY, boxes.getLast().minY, DELTA, "the sweep left the level");
        assertTrue(CrashBreach.getBoxes(BODY, new Vec3(0.0D, 1.0D, 0.0D), 2.0D).isEmpty(), "a straight-up way swept");
        assertTrue(CrashBreach.getBoxes(BODY, new Vec3(1.0D, 0.0D, 0.0D), 0.0D).isEmpty(), "no length swept");
    }

    @Test
    @DisplayName("a standing body meets one column two blocks high, never the floor")
    void aBodyMeetsOneColumnTwoHigh() {
        List<BlockPos> cells = CrashBreach.getCells(CrashBreach.getBoxes(BODY, new Vec3(0.0D, 0.0D, 1.0D), 1.0D)
                .getFirst());
        assertEquals(List.of(new BlockPos(4, 1, 0), new BlockPos(4, 2, 0)), cells);
    }

    @Test
    @DisplayName("points pick the shatter tier and the two boars reach the top")
    void theHardnessFollowsPointsAndTheBoars() {
        assertEquals(AreaDamageShatter.NONE, CrashBreach.getMaxHardness(56, false), "56 points broke a block");
        assertEquals(AreaDamageShatter.NONE, CrashBreach.getMaxHardness(40, false), "40 points broke a block");
        assertEquals(0.6F, CrashBreach.getMaxHardness(100, false));
        assertEquals(0.6F, CrashBreach.getMaxHardness(168, false));
        assertEquals(1.5F, CrashBreach.getMaxHardness(200, false));
        assertEquals(3.0F, CrashBreach.getMaxHardness(400, false));
        assertEquals(3.0F, CrashBreach.getMaxHardness(0, true));
        assertEquals(3.0F, CrashBreach.getMaxHardness(48, true));
        assertTrue(AreaDamageShatter.canBreak(1.5F, false, CrashBreach.getMaxHardness(0, true)));
        assertFalse(AreaDamageShatter.canBreak(4.5F, false, CrashBreach.getMaxHardness(0, true)),
                "the boars broke cobbled deepslate's 3.5 or more");
    }
}
