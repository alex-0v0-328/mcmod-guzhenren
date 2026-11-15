package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaImpactShatterTest {

    private static final Vec3 EYES = new Vec3(0.5D, 65.62D, 0.5D);
    private static final Vec3 FEET = new Vec3(0.5D, 64.0D, 0.5D);
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final double HEIGHT = 1.8D;

    @Test
    @DisplayName("no shatter under 100 points, then soil, stone and hardness-3 blocks at 100, 200 and 400")
    void hardnessFollowsThePoints() {
        assertEquals(AreaImpactShatter.NONE, AreaImpactShatter.getMaxHardness(99));
        assertEquals(0.6F, AreaImpactShatter.getMaxHardness(100));
        assertEquals(0.6F, AreaImpactShatter.getMaxHardness(199));
        assertEquals(1.5F, AreaImpactShatter.getMaxHardness(200));
        assertEquals(3.0F, AreaImpactShatter.getMaxHardness(400));
        assertEquals(3.0F, AreaImpactShatter.getMaxHardness(10_000));
    }

    @Test
    @DisplayName("a block breaks up to the cap, never with a block entity or when unbreakable")
    void breakableUpToTheCap() {
        assertTrue(AreaImpactShatter.canBreak(0.0F, false, 0.6F));
        assertTrue(AreaImpactShatter.canBreak(0.6F, false, 0.6F));
        assertFalse(AreaImpactShatter.canBreak(1.5F, false, 0.6F));
        assertFalse(AreaImpactShatter.canBreak(2.5F, true, 3.0F));
        assertFalse(AreaImpactShatter.canBreak(-1.0F, false, 3.0F));
        assertFalse(AreaImpactShatter.canBreak(0.0F, false, AreaImpactShatter.NONE));
    }

    @Test
    @DisplayName("cone cells sit inside the cone, nearest first, nothing behind the eyes")
    void coneCellsAreInsideAndNearestFirst() {
        List<BlockPos> cells = AreaImpactShatter.getConeCells(EYES, EAST, 4.0D);
        assertFalse(cells.isEmpty());
        for (BlockPos pos : cells) {
            assertTrue(AreaImpact.isInCone(EYES, EAST, 4.0D, pos.getCenter()), pos + " lies outside the cone");
        }
        for (int i = 1; i < cells.size(); i++) {
            assertTrue(cells.get(i - 1).getCenter().distanceToSqr(EYES) <= cells.get(i).getCenter().distanceToSqr(EYES),
                    "the cells are not nearest first at " + i);
        }
        assertTrue(cells.contains(new BlockPos(3, 65, 0)), "the cell straight ahead is missing");
        assertFalse(cells.contains(new BlockPos(5, 65, 0)), "a cell past the radius slipped in");
        assertFalse(cells.contains(new BlockPos(-1, 65, 0)), "a cell behind the eyes slipped in");
        assertTrue(AreaImpactShatter.getConeCells(EYES, Vec3.ZERO, 4.0D).isEmpty());
    }

    @Test
    @DisplayName("fan cells run from the feet layer to a block over the head, never the ground or the own column")
    void fanCellsSpanTheBodyAndSpareTheGround() {
        List<BlockPos> cells = AreaImpactShatter.getFanCells(FEET, HEIGHT, EAST, 3.0D, 45.0D, EYES);
        assertTrue(cells.contains(new BlockPos(2, 64, 0)), "the feet layer ahead is missing");
        assertTrue(cells.contains(new BlockPos(2, 66, 0)), "the layer over the head is missing");
        assertFalse(cells.contains(new BlockPos(2, 63, 0)), "the ground under the feet slipped in");
        assertFalse(cells.contains(new BlockPos(2, 67, 0)), "a layer too high slipped in");
        assertFalse(cells.contains(new BlockPos(0, 66, 0)), "the own column slipped in");
        assertFalse(cells.contains(new BlockPos(4, 64, 0)), "a cell past the radius slipped in");
        assertFalse(cells.contains(new BlockPos(0, 64, 2)), "a cell 90 degrees aside slipped in");
        assertTrue(cells.getFirst().getCenter().distanceToSqr(EYES)
                <= cells.getLast().getCenter().distanceToSqr(EYES), "the cells are not nearest first");
    }

    @Test
    @DisplayName("standing on a slab the fan still starts at the layer above the slab")
    void onASlabTheFanSparesTheSlabLayer() {
        Vec3 feet = new Vec3(0.5D, 63.5D, 0.5D);
        List<BlockPos> cells = AreaImpactShatter.getFanCells(feet, HEIGHT, EAST, 3.0D, 45.0D, EYES);
        assertFalse(cells.contains(new BlockPos(2, 63, 0)), "the slab layer slipped in");
        assertTrue(cells.contains(new BlockPos(2, 64, 0)));
        assertTrue(cells.contains(new BlockPos(2, 66, 0)));
    }
}
