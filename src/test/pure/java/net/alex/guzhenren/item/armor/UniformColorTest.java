package net.alex.guzhenren.item.armor;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniformColorTest {

    @Test
    void everyDyeColorMapsToDistinctOpaqueHistoricalColor() {
        Set<Integer> seen = new HashSet<>();
        for (DyeColor color : DyeColor.values()) {
            int rgb = UniformColor.rgb(color);
            assertTrue(rgb >= 0 && rgb <= 0xFFFFFF, () -> "RGB out of range for " + color);
            assertTrue(seen.add(rgb), () -> "Duplicate RGB for " + color);
            assertEquals(0xFF000000 | rgb, UniformColor.argb(color));
            assertEquals("guzhenren.uniform_color." + color.getName(), UniformColor.translationKey(color));
        }
        assertEquals(16, seen.size());
    }

    @Test
    void defaultColorIsBlackAndMatchesHandoffPalette() {
        assertEquals(DyeColor.BLACK, UniformColor.DEFAULT);
        assertEquals(0x2D313C, UniformColor.rgb(DyeColor.BLACK));
        assertEquals(0x445E82, UniformColor.rgb(DyeColor.BLUE));
        assertEquals(0x5F537D, UniformColor.rgb(DyeColor.PURPLE));
        assertEquals(0xD4CCB8, UniformColor.rgb(DyeColor.WHITE));
    }
}
