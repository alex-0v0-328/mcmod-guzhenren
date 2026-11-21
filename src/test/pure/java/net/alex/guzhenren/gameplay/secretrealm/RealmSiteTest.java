package net.alex.guzhenren.gameplay.secretrealm;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmSiteTest {

    private static final RealmSite SITE = new RealmSite(new BlockPos(1000, 64, -2000), 96);

    @Test
    @DisplayName("the range is measured from the centre of the centre block and includes its rim")
    void rangeStartsAtTheBlockCentre() {
        assertTrue(SITE.contains(1000.5D, -1999.5D));
        assertTrue(SITE.contains(1000.5D + 96.0D, -1999.5D));
        assertFalse(SITE.contains(1000.5D + 96.01D, -1999.5D));
        assertTrue(SITE.contains(1000.5D, -1999.5D - 96.0D));
    }

    @Test
    @DisplayName("the range is a circle, so a box corner inside 96 on both axes is outside")
    void rangeIsACircleNotASquare() {
        assertTrue(SITE.contains(1000.5D + 67.0D, -1999.5D + 67.0D));
        assertFalse(SITE.contains(1000.5D + 70.0D, -1999.5D + 70.0D));
    }

    @Test
    @DisplayName("the wider player gate compares the same centre with a larger distance")
    void widerDistanceForThePlayerGate() {
        int gate = SITE.radius() + SecretRealms.PLAYER_MARGIN;
        assertFalse(SITE.contains(1000.5D + 120.0D, -1999.5D));
        assertTrue(SITE.isWithin(1000.5D + 120.0D, -1999.5D, gate));
        assertFalse(SITE.isWithin(1000.5D + 129.0D, -1999.5D, gate));
    }
}
