package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTerrain;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;

/**
 * 地图测试用的地图片工厂：按「列 → 地面高 / 地貌」函数造一片区块（每列一段往下实心的地面，树叶列高出 3 格），存进可当
 * {@link TileSource} 用的表；{@link #tree}、{@link #floating} 再往单列里加段。
 */

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
        MapTile.Builder builder = new MapTile.Builder();
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = chunkX * 16 + dx;
                int z = chunkZ * 16 + dz;
                int index = MapTile.index(x, z);
                MapTerrain kind = terrain.at(x, z);
                int height = ground.ground(x, z);
                builder.ground(index, height).run(index, height + (kind == MapTerrain.FOLIAGE ? 3 : 0), MapTile.OPEN,
                        kind);
                if (kind == MapTerrain.WATER) builder.flag(index, MapTile.FLAG_FLUID);
            }
        }
        return builder.build(chunkX, chunkZ);
    }

    /** 在已有地图片里种一棵树：{@code (x, z)} 列的树冠从 {@code low} 到 {@code top}，{@code trunk} 为真时下面立树干。 */
    MapTestTiles tree(int x, int z, int low, int top, boolean trunk) {
        MapTile tile = tile(x >> 4, z >> 4);
        int index = MapTile.index(x, z);
        int ground = tile.ground()[index];
        MapTerrain base = tile.runTerrain(tile.run(index, 0));
        return replace(x, z, builder -> {
            builder.run(index, top, low, MapTerrain.FOLIAGE);
            if (trunk) builder.run(index, low - 1, ground + 1, MapTerrain.WOOD);
            builder.run(index, ground, MapTile.OPEN, base);
        });
    }

    /** 在 {@code (x, z)} 列地面上方悬一段 {@code [bottom, top]} 的方块，地面与地面高都不动。 */
    MapTestTiles floating(int x, int z, int bottom, int top, MapTerrain terrain) {
        MapTile tile = tile(x >> 4, z >> 4);
        int index = MapTile.index(x, z);
        return replace(x, z, builder -> {
            builder.run(index, top, bottom, terrain);
            for (int k = 0; k < tile.runs(index); k++) {
                int run = tile.run(index, k);
                builder.run(index, tile.runTop(run), tile.runBottom(run), tile.runTerrain(run));
            }
        });
    }

    private MapTestTiles replace(int x, int z, Consumer<MapTile.Builder> column) {
        MapTile tile = tile(x >> 4, z >> 4);
        int replaced = MapTile.index(x, z);
        MapTile.Builder builder = new MapTile.Builder();
        for (int index = 0; index < MapTile.COLUMNS; index++) {
            builder.ground(index, tile.ground()[index]).flag(index, tile.flags()[index]);
            if (index == replaced) {
                column.accept(builder);
                continue;
            }
            for (int k = 0; k < tile.runs(index); k++) {
                int run = tile.run(index, k);
                builder.run(index, tile.runTop(run), tile.runBottom(run), tile.runTerrain(run));
            }
        }
        return put(builder.build(tile.chunkX(), tile.chunkZ()));
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
