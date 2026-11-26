package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;

/** 地图测试用的地图片工厂：按「列 → 地面高 / 地貌」函数造一片区块，存进可当 {@link TileSource} 用的表。 */

final class MapTestTiles implements TileSource {

    private final Map<Long, MapTile> tiles = new HashMap<>();

    interface Column {

        int ground(int x, int z);
    }

    interface Terrain {

        MapTerrain at(int x, int z);
    }

    MapTestTiles fill(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, Column ground, Terrain terrain) {
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                put(tile(chunkX, chunkZ, ground, terrain));
            }
        }
        return this;
    }

    MapTestTiles put(MapTile tile) {
        tiles.put(key(tile.chunkX(), tile.chunkZ()), tile);
        return this;
    }

    static MapTile tile(int chunkX, int chunkZ, Column ground, Terrain terrain) {
        short[] grounds = new short[MapTile.COLUMNS];
        short[] surfaces = new short[MapTile.COLUMNS];
        byte[] terrains = new byte[MapTile.COLUMNS];
        byte[] flags = new byte[MapTile.COLUMNS];
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = chunkX * 16 + dx;
                int z = chunkZ * 16 + dz;
                int index = MapTile.index(x, z);
                MapTerrain kind = terrain.at(x, z);
                grounds[index] = (short) ground.ground(x, z);
                surfaces[index] = (short) (ground.ground(x, z) + (kind == MapTerrain.FOLIAGE ? 3 : 0));
                terrains[index] = kind.id();
                flags[index] = (byte) (kind == MapTerrain.WATER ? MapTile.FLAG_FLUID : 0);
            }
        }
        return new MapTile(chunkX, chunkZ, grounds, surfaces, terrains, flags);
    }

    MapGrid grid(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        return MapGrid.capture(this, new MapView(minChunkX, minChunkZ, maxChunkX, maxChunkZ));
    }

    @Override
    public MapTile tile(int chunkX, int chunkZ) { return tiles.get(key(chunkX, chunkZ)); }

    private static long key(int chunkX, int chunkZ) { return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL; }

    static int hash(int x, int z) {
        int value = x * 73_856_093 ^ z * 19_349_663;
        value ^= value >>> 13;
        value *= 0x5BD1E995;
        return value ^ value >>> 15;
    }
}
