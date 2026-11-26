package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapTileTest {

    private static MapTile sample(int chunkX, int chunkZ) {
        short[] ground = new short[MapTile.COLUMNS];
        short[] surface = new short[MapTile.COLUMNS];
        byte[] terrain = new byte[MapTile.COLUMNS];
        byte[] flags = new byte[MapTile.COLUMNS];
        for (int i = 0; i < MapTile.COLUMNS; i++) {
            ground[i] = (short) (-64 + i);
            surface[i] = (short) (ground[i] + i % 7);
            terrain[i] = MapTerrain.values()[2 + i % (MapTerrain.values().length - 2)].id();
            flags[i] = (byte) (i % 4);
        }
        return new MapTile(chunkX, chunkZ, ground, surface, terrain, flags);
    }

    private static MapTile roundTrip(MapTile tile) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            MapTile.STREAM_CODEC.encode(buffer, tile);
            MapTile decoded = MapTile.STREAM_CODEC.decode(buffer);
            assertEquals(0, buffer.readableBytes(), "the codec reads back exactly what it wrote");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    @Test
    @DisplayName("a full tile survives the codec column for column, negative chunks and heights included")
    void fullTileRoundTrip() {
        MapTile tile = sample(-37, 1204);
        MapTile decoded = roundTrip(tile);
        assertTrue(tile.sameAs(decoded));
        assertEquals(tile.hash(), decoded.hash());
        assertEquals(-64, decoded.groundAt(0, 0));
        assertEquals(MapTerrain.byId(tile.terrain()[MapTile.index(5, 9)]), decoded.terrainAt(5, 9));
    }

    @Test
    @DisplayName("a void tile is sent as a marker byte and comes back all void")
    void emptyTileRoundTrip() {
        MapTile tile = MapTile.empty(612, -612);
        ByteBuf buffer = Unpooled.buffer();
        try {
            MapTile.STREAM_CODEC.encode(buffer, tile);
            assertTrue(buffer.readableBytes() <= 11, "two varints and a marker byte at most, not 1.5 KB");
        } finally {
            buffer.release();
        }
        MapTile decoded = roundTrip(tile);
        assertTrue(decoded.isEmpty());
        assertEquals(MapTerrain.VOID, decoded.terrainAt(15, 15));
        assertEquals(612, decoded.chunkX());
        assertEquals(-612, decoded.chunkZ());
    }

    @Test
    @DisplayName("columns are indexed z-major and wrap world coordinates into the chunk")
    void columnIndex() {
        assertEquals(0, MapTile.index(0, 0));
        assertEquals(15, MapTile.index(15, 0));
        assertEquals(16, MapTile.index(0, 1));
        assertEquals(255, MapTile.index(-1, -1));
        assertEquals(MapTile.index(3, 4), MapTile.index(16 * 5 + 3, -16 * 2 + 4));
    }

    @Test
    @DisplayName("the content hash moves when one column changes and the flags read back per bit")
    void hashAndFlags() {
        MapTile tile = sample(1, 2);
        MapTile changed = sample(1, 2);
        changed.surface()[17]++;
        assertNotEquals(tile.hash(), changed.hash());
        assertFalse(tile.sameAs(changed));
        assertTrue(tile.hasFlag(1, 0, MapTile.FLAG_FLUID));
        assertFalse(tile.hasFlag(1, 0, MapTile.FLAG_BLOCK_ENTITY));
        assertTrue(tile.hasFlag(3, 0, MapTile.FLAG_FLUID | MapTile.FLAG_BLOCK_ENTITY));
    }

    @Test
    @DisplayName("a tile must have exactly 256 columns in every array")
    void columnCountIsChecked() {
        assertThrows(IllegalArgumentException.class, () -> new MapTile(0, 0, new short[255],
                new short[MapTile.COLUMNS], new byte[MapTile.COLUMNS], new byte[MapTile.COLUMNS]));
    }
}
