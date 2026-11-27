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

    private static MapTile sample(int chunkX, int chunkZ, int bump) {
        MapTile.Builder builder = new MapTile.Builder();
        for (int i = 0; i < MapTile.COLUMNS; i++) {
            int ground = -64 + i;
            builder.ground(i, ground).flag(i, i % 4);
            for (int k = i % 4; k > 0; k--) {
                builder.run(i, ground + 10 * k + bump, ground + 10 * k - 2, MapTerrain.values()[2 + (i + k) % 20]);
            }
            if (i % 5 != 0) builder.run(i, ground, i % 3 == 0 ? ground - 30 : MapTile.OPEN, MapTerrain.STONE);
        }
        return builder.build(chunkX, chunkZ);
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
    @DisplayName("a full tile survives the codec run for run, negative chunks, heights and open bottoms included")
    void fullTileRoundTrip() {
        MapTile tile = sample(-37, 1204, 0);
        MapTile decoded = roundTrip(tile);
        assertTrue(tile.sameAs(decoded));
        assertEquals(tile.hash(), decoded.hash());
        assertEquals(-64, decoded.groundAt(0, 0));
        int index = MapTile.index(7, 0);
        assertEquals(4, decoded.runs(index));
        assertEquals(-57 + 30, decoded.runTop(decoded.run(index, 0)));
        assertTrue(decoded.isOpen(decoded.run(index, 3)));
        assertFalse(decoded.isOpen(decoded.run(MapTile.index(3, 0), 3)));
        assertEquals(-61 - 30, decoded.runBottom(decoded.run(MapTile.index(3, 0), 3)));
        assertEquals(MapTerrain.STONE, decoded.runTerrain(decoded.run(index, 3)));
    }

    @Test
    @DisplayName("a column without runs is void, a tile without runs is sent as a marker byte and comes back all void")
    void emptyTileRoundTrip() {
        MapTile tile = MapTile.empty(612, -612);
        assertEquals(MapTerrain.VOID, tile.terrainAt(4, 4));
        ByteBuf buffer = Unpooled.buffer();
        try {
            MapTile.STREAM_CODEC.encode(buffer, tile);
            assertTrue(buffer.readableBytes() <= 11, "two varints and a marker byte at most, not kilobytes");
        } finally {
            buffer.release();
        }
        MapTile decoded = roundTrip(tile);
        assertTrue(decoded.isEmpty());
        assertEquals(0, decoded.runs(MapTile.index(15, 15)));
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
    @DisplayName("the content hash moves when one run changes and the flags read back per bit")
    void hashAndFlags() {
        MapTile tile = sample(1, 2, 0);
        MapTile changed = sample(1, 2, 1);
        assertNotEquals(tile.hash(), changed.hash());
        assertFalse(tile.sameAs(changed));
        assertTrue(tile.hasFlag(1, 0, MapTile.FLAG_FLUID));
        assertFalse(tile.hasFlag(1, 0, MapTile.FLAG_BLOCK_ENTITY));
        assertTrue(tile.hasFlag(3, 0, MapTile.FLAG_FLUID | MapTile.FLAG_BLOCK_ENTITY));
    }

    @Test
    @DisplayName("a column holds at most 16 runs, in the builder and on the wire")
    void runCountIsCapped() {
        MapTile.Builder builder = new MapTile.Builder();
        for (int k = 0; k < MapTile.MAX_RUNS; k++) builder.run(0, 100 - 2 * k, 100 - 2 * k, MapTerrain.STONE);
        assertThrows(IllegalStateException.class, () -> builder.run(0, 60, 60, MapTerrain.STONE));
        ByteBuf buffer = Unpooled.buffer();
        try {
            MapTile.STREAM_CODEC.encode(buffer, builder.build(0, 0));
            buffer.setByte(2 + 1 + 2 + 1, MapTile.MAX_RUNS + 1);
            assertThrows(IllegalArgumentException.class, () -> MapTile.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    @DisplayName("a tile must have exactly 256 columns and run arrays that match its offsets")
    void shapeIsChecked() {
        assertThrows(IllegalArgumentException.class, () -> new MapTile(0, 0, new short[255], new byte[256],
                new short[257], new short[0], new short[0], new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new MapTile(0, 0, new short[256], new byte[256],
                new short[257], new short[1], new short[1], new byte[1]));
    }
}
