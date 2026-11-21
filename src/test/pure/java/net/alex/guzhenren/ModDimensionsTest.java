package net.alex.guzhenren;

import java.util.List;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDimensionsTest {

    @Test
    @DisplayName("the allow-list pins Treasure Yellow Heaven: fixed spawn in flight, rank 8 grotto-heaven")
    void anchoredDimensionsPinTreasureYellowHeaven() {
        assertEquals(6, ModDimensions.ANCHORED_DIMENSIONS.size());
        ModDimensions.AnchoredDimension entry =
                ModDimensions.ANCHORED_DIMENSIONS.get(ModDimensions.TREASURE_YELLOW_HEAVEN);
        assertNotNull(entry);
        assertEquals(new Vec3(0.5, 64.0, 0.5), entry.spawn());
        assertEquals(ModDimensions.Arrival.FLIGHT, entry.arrival());
        assertEquals(8, entry.rank());
    }

    @Test
    @DisplayName("the allow-list pins the four regions: on foot at the surface above their origin, no rank")
    void anchoredDimensionsPinTheRegions() {
        for (ResourceKey<Level> region : List.of(ModDimensions.SOUTHERN_BORDER, ModDimensions.WESTERN_DESERT,
                ModDimensions.EASTERN_SEA, ModDimensions.NORTHERN_PLAINS)) {
            ModDimensions.AnchoredDimension entry = ModDimensions.ANCHORED_DIMENSIONS.get(region);
            assertNotNull(entry, region.toString());
            assertEquals(0.5, entry.spawn().x);
            assertEquals(0.5, entry.spawn().z);
            assertEquals(ModDimensions.Arrival.SURFACE, entry.arrival());
            assertEquals(0, entry.rank());
            assertFalse(ModDimensions.isFlightGranted(region));
        }
    }

    @Test
    @DisplayName("only a flight dimension keeps the flight grant: Treasure Yellow Heaven, not the Southern Border")
    void onlyFlightDimensionsGrantFlight() {
        assertTrue(ModDimensions.isFlightGranted(ModDimensions.TREASURE_YELLOW_HEAVEN));
        assertFalse(ModDimensions.isFlightGranted(ModDimensions.SOUTHERN_BORDER));
        assertFalse(ModDimensions.isFlightGranted(Level.OVERWORLD));
    }

    @Test
    @DisplayName("every level key is Gu World's: that mod supplies the dimensions")
    void anchoredDimensionsBelongToTheWorldMod() {
        assertEquals("guworld:treasure_yellow_heaven",
                ModDimensions.TREASURE_YELLOW_HEAVEN.location().toString());
        assertEquals("guworld:southern_border", ModDimensions.SOUTHERN_BORDER.location().toString());
        assertEquals("guworld:western_desert", ModDimensions.WESTERN_DESERT.location().toString());
        assertEquals("guworld:eastern_sea", ModDimensions.EASTERN_SEA.location().toString());
        assertEquals("guworld:northern_plains", ModDimensions.NORTHERN_PLAINS.location().toString());
        assertEquals("guworld:fox_immortal_blissful_land",
                ModDimensions.FOX_IMMORTAL_BLISSFUL_LAND.location().toString());
    }

    @Test
    @DisplayName("the Fox Immortal Blissful Land lands on foot 160 blocks south of the summit column, rank 6")
    void anchoredDimensionsPinTheFoxImmortalBlissfulLand() {
        ModDimensions.AnchoredDimension entry =
                ModDimensions.ANCHORED_DIMENSIONS.get(ModDimensions.FOX_IMMORTAL_BLISSFUL_LAND);
        assertNotNull(entry);
        assertEquals(8.5, entry.spawn().x);
        assertEquals(168.5, entry.spawn().z);
        assertEquals(ModDimensions.Arrival.SURFACE, entry.arrival());
        assertEquals(6, entry.rank());
        assertFalse(ModDimensions.isFlightGranted(ModDimensions.FOX_IMMORTAL_BLISSFUL_LAND));
    }

    @Test
    @DisplayName("anyone inside Treasure Yellow Heaven or the Fox land may exit by themself; the regions stay admin-only")
    void selfExitDimensions() {
        assertTrue(ModDimensions.allowsSelfExit(ModDimensions.TREASURE_YELLOW_HEAVEN));
        assertTrue(ModDimensions.allowsSelfExit(ModDimensions.FOX_IMMORTAL_BLISSFUL_LAND));
        assertFalse(ModDimensions.allowsSelfExit(ModDimensions.SOUTHERN_BORDER));
        assertFalse(ModDimensions.allowsSelfExit(Level.OVERWORLD));
    }
}
