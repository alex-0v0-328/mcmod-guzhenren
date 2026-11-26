package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LandSiteTest {

    @Test
    @DisplayName("the summit, palace and entry follow the mountain centre wherever it lands")
    void everythingIsRelativeToTheCentre() {
        LandSite old = LandSite.of(new RealmSite(new BlockPos(8, 64, 8), 96), SoulsunderMountain.SUMMIT_RISE,
                ModDimensions.BLISSFUL_LAND_FOX_IMMORTAL_SPAWN);
        assertEquals(181, old.summitY());
        assertEquals(new BlockPos(6, 104, 5), old.palace());
        assertEquals(new BlockPos(8, 64, 168), old.entry());

        LandSite moved = LandSite.of(new RealmSite(new BlockPos(-312, 97, 456), 96), SoulsunderMountain.SUMMIT_RISE,
                ModDimensions.BLISSFUL_LAND_FOX_IMMORTAL_SPAWN);
        assertEquals(214, moved.summitY());
        assertEquals(new BlockPos(-314, 137, 453), moved.palace());
        assertEquals(new BlockPos(-312, 97, 616), moved.entry());
        assertEquals(new BlockPos(-312, 97, 456), moved.center());
    }
}
