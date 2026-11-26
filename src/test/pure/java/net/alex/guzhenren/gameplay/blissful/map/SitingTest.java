package net.alex.guzhenren.gameplay.blissful.map;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SitingTest {

    private static final Territory LAND = Territory.whole(64, 4);

    private static final class World implements Siting.Tiles {

        private final Map<Long, MapTile> tiles = new HashMap<>();

        World flat(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, int ground) {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    short[] heights = new short[MapTile.COLUMNS];
                    Arrays.fill(heights, (short) ground);
                    tiles.put(ChunkPos.asLong(x, z), new MapTile(x, z, heights, heights.clone(),
                            new byte[MapTile.COLUMNS], new byte[MapTile.COLUMNS]));
                }
            }
            return this;
        }

        World column(int x, int z, int ground, int flags) {
            MapTile tile = tiles.get(ChunkPos.asLong(x >> 4, z >> 4));
            tile.ground()[MapTile.index(x, z)] = (short) ground;
            tile.flags()[MapTile.index(x, z)] = (byte) flags;
            return this;
        }

        @Override
        public MapTile at(int chunkX, int chunkZ) { return tiles.get(ChunkPos.asLong(chunkX, chunkZ)); }
    }

    private static Siting.Plot plot(List<Long> fields, List<Long> pastures) {
        return Siting.Plot.of(LAND, 0, 0, 0, fields, pastures);
    }

    private static Siting.Plot empty() { return plot(List.of(), List.of()); }

    @Test
    @DisplayName("a dry, flat chunk in the territory takes a field; a flat two by two takes a pasture")
    void ok() {
        World world = new World().flat(10, 10, 11, 11, 70).column(165, 165, 73, 0);
        assertEquals(Siting.Reason.OK, Siting.field(empty(), world, 10, 10), "three blocks of rise is allowed");
        assertEquals(Siting.Reason.OK, Siting.pasture(empty(), world, 10, 10));
    }

    @Test
    @DisplayName("fields and plans stop at 64, pastures at 8, before the spot is looked at")
    void cap() {
        Siting.Plot full = new Siting.Plot(LAND, 0, 0, 0, new it.unimi.dsi.fastutil.longs.LongOpenHashSet(),
                Siting.FIELD_CAP, Siting.PASTURE_CAP);
        assertEquals(Siting.Reason.CAP, Siting.field(full, new World(), 10, 10));
        assertEquals(Siting.Reason.CAP, Siting.pasture(full, new World(), 10, 10));
    }

    @Test
    @DisplayName("a chunk outside the territory, or a pasture hanging over a ceded edge, is out of bounds")
    void outOfBounds() {
        World world = new World().flat(62, 0, 64, 1, 70);
        assertEquals(Siting.Reason.OUT_OF_BOUNDS, Siting.field(empty(), world, 64, 0));
        assertEquals(Siting.Reason.OUT_OF_BOUNDS, Siting.pasture(empty(), world, 63, 0), "its east half is outside");
        Siting.Plot ceded = Siting.Plot.of(LAND.cede(Territory.Side.EAST, 2), 0, 0, 0, List.of(), List.of());
        assertEquals(Siting.Reason.OUT_OF_BOUNDS, Siting.field(ceded, world, 62, 0));
    }

    @Test
    @DisplayName("any block of the chunk within the realm radius is refused")
    void realm() {
        World world = new World().flat(0, 0, 7, 0, 70);
        Siting.Plot plot = Siting.Plot.of(LAND, 8, 8, 96, List.of(), List.of());
        assertEquals(Siting.Reason.REALM, Siting.field(plot, world, 6, 0), "x 96 is 87.5 from the centre");
        assertEquals(Siting.Reason.OK, Siting.field(plot, world, 7, 0), "x 112 is 103.5 from the centre");
    }

    @Test
    @DisplayName("a chunk held by a field, a plan or any of a pasture's four chunks overlaps; a neighbour does not")
    void overlap() {
        World world = new World().flat(0, 0, 5, 5, 70);
        Siting.Plot plot = plot(List.of(ChunkPos.asLong(0, 0)), List.of(ChunkPos.asLong(2, 2)));
        assertEquals(Siting.Reason.OVERLAP, Siting.field(plot, world, 0, 0));
        assertEquals(Siting.Reason.OVERLAP, Siting.field(plot, world, 3, 3), "the pasture's south-east chunk");
        assertEquals(Siting.Reason.OK, Siting.field(plot, world, 1, 0), "fields may sit side by side");
        assertEquals(Siting.Reason.OVERLAP, Siting.pasture(plot, world, 1, 1));
        assertEquals(Siting.Reason.OK, Siting.pasture(plot.without(2, 2), world, 3, 3), "moving drops its own chunks");
    }

    @Test
    @DisplayName("a chunk the map has never seen is fog")
    void fog() {
        World world = new World().flat(0, 0, 0, 1, 70);
        assertEquals(Siting.Reason.FOG, Siting.field(empty(), world, 5, 5));
        assertEquals(Siting.Reason.FOG, Siting.pasture(empty(), world, 0, 0), "two of its four chunks are unseen");
    }

    @Test
    @DisplayName("any fluid column spoils a field; a pasture minds only its fence line")
    void fluid() {
        World world = new World().flat(0, 0, 1, 1, 70).column(8, 8, 69, MapTile.FLAG_FLUID);
        assertEquals(Siting.Reason.FLUID, Siting.field(empty(), world, 0, 0));
        assertEquals(Siting.Reason.OK, Siting.pasture(empty(), world, 0, 0), "a pond inside the fence is fine");
        world.column(1, 20, 69, MapTile.FLAG_FLUID);
        assertEquals(Siting.Reason.FLUID, Siting.pasture(empty(), world, 0, 0), "water on the fence line is not");
    }

    @Test
    @DisplayName("a block entity spoils a field but not a pasture")
    void blockEntity() {
        World world = new World().flat(0, 0, 1, 1, 70).column(3, 3, 70, MapTile.FLAG_BLOCK_ENTITY);
        assertEquals(Siting.Reason.BLOCK_ENTITY, Siting.field(empty(), world, 0, 0));
        assertEquals(Siting.Reason.OK, Siting.pasture(empty(), world, 0, 0));
    }

    @Test
    @DisplayName("more than three blocks of rise spoils a field, more than eight a pasture")
    void slope() {
        World world = new World().flat(0, 0, 1, 1, 70).column(15, 15, 74, 0);
        assertEquals(Siting.Reason.SLOPE, Siting.field(empty(), world, 0, 0));
        assertEquals(Siting.Reason.OK, Siting.pasture(empty(), world, 0, 0));
        world.column(20, 20, 78, 0);
        assertEquals(Siting.Reason.OK, Siting.pasture(empty(), world, 0, 0), "eight blocks of rise is allowed");
        world.column(20, 20, 79, 0);
        assertEquals(Siting.Reason.SLOPE, Siting.pasture(empty(), world, 0, 0));
        world.column(20, 20, 70, 0).column(0, 0, 90, 0);
        assertEquals(Siting.Reason.OK, Siting.pasture(empty(), world, 0, 0), "the margin outside the fence is free");
    }
}
