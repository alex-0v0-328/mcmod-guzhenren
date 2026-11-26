package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import net.minecraft.world.level.material.MapColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapTerrainTest {

    @Test
    @DisplayName("terrain ids are the wire bytes: unknown and void lead, other stays last")
    void wireOrder() {
        assertEquals(0, MapTerrain.UNKNOWN.id());
        assertEquals(1, MapTerrain.VOID.id());
        assertEquals(MapTerrain.values().length - 1, MapTerrain.OTHER.id());
        assertEquals(MapTerrain.GUTS_STONE, MapTerrain.byId(MapTerrain.GUTS_STONE.id()));
        assertEquals(MapTerrain.UNKNOWN, MapTerrain.byId(-1));
        assertEquals(MapTerrain.UNKNOWN, MapTerrain.byId(MapTerrain.values().length));
    }

    @Test
    @DisplayName("blocks no rule names fall back to their vanilla map colour")
    void mapColourFallback() {
        assertEquals(MapTerrain.GRASS, MapTerrain.ofMapColor(MapColor.GRASS));
        assertEquals(MapTerrain.FOLIAGE, MapTerrain.ofMapColor(MapColor.PLANT));
        assertEquals(MapTerrain.WATER, MapTerrain.ofMapColor(MapColor.WATER));
        assertEquals(MapTerrain.LAVA, MapTerrain.ofMapColor(MapColor.FIRE));
        assertEquals(MapTerrain.SAND, MapTerrain.ofMapColor(MapColor.SAND));
        assertEquals(MapTerrain.SNOW, MapTerrain.ofMapColor(MapColor.SNOW));
        assertEquals(MapTerrain.ICE, MapTerrain.ofMapColor(MapColor.ICE));
        assertEquals(MapTerrain.DIRT, MapTerrain.ofMapColor(MapColor.PODZOL));
        assertEquals(MapTerrain.WOOD, MapTerrain.ofMapColor(MapColor.WOOD));
        assertEquals(MapTerrain.STONE, MapTerrain.ofMapColor(MapColor.DEEPSLATE));
        assertEquals(MapTerrain.STONE, MapTerrain.ofMapColor(MapColor.TERRACOTTA_CYAN));
        assertEquals(MapTerrain.OTHER, MapTerrain.ofMapColor(MapColor.COLOR_MAGENTA));
        assertEquals(MapTerrain.OTHER, MapTerrain.ofMapColor(MapColor.NONE));
    }
}
