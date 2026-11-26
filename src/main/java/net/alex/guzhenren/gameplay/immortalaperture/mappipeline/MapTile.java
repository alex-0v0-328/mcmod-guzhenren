package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 一个区块的地表摘要（地图片[Map Tile]）：服务端 {@link MapScanner} 扫出，经地图片载荷下发，客户端地图按它画。
 * <ul>
 *   <li>四个数组都是 {@value #COLUMNS} 列，列下标 = {@code (z & 15) * 16 + (x & 15)}（{@link #index}）；
 *       {@code ground} 是真实地面 y、{@code surface} 是最高非空气方块 y、{@code terrain} 是 {@link MapTerrain} 序号、
 *       {@code flags} 是 {@link #FLAG_FLUID} 与 {@link #FLAG_BLOCK_ENTITY} 的位或；后者只认该列地面下 8 格到上 32 格
 *       内的方块实体（{@link MapScanner#BLOCK_ENTITY_BELOW}、{@link MapScanner#BLOCK_ENTITY_ABOVE}）。</li>
 *   <li>⚠ 数组按引用持有、不拷贝，造出来后谁都不许改；记录的 {@code equals} 只比引用，比内容用 {@link #sameAs}，
 *       {@link #hash} 是内容哈希（服务端拿它判「变没变」）。</li>
 *   <li>{@link #STREAM_CODEC}：区块坐标两个 varint，再按列写 {@code ground}、{@code surface}（各 2 字节）、
 *       {@code terrain}、{@code flags}（各 1 字节）；整片 {@link MapTerrain#VOID} 的只写坐标加一个标记字节，
 *       满片约 1.5 KB，每包 32 片约 48 KB；标记字节不认识的抛异常（坏包）。</li>
 * </ul>
 */

public record MapTile(int chunkX, int chunkZ, short[] ground, short[] surface, byte[] terrain, byte[] flags) {

    public static final int COLUMNS = 256;
    public static final int FLAG_FLUID = 1;
    public static final int FLAG_BLOCK_ENTITY = 2;
    private static final byte FULL = 0;
    private static final byte EMPTY = 1;
    public static final StreamCodec<ByteBuf, MapTile> STREAM_CODEC = StreamCodec.of(MapTile::write, MapTile::read);

    public MapTile {
        if (ground.length != COLUMNS || surface.length != COLUMNS || terrain.length != COLUMNS
                || flags.length != COLUMNS) {
            throw new IllegalArgumentException("a map tile has " + COLUMNS + " columns");
        }
    }

    public static int index(int x, int z) { return (z & 15) << 4 | x & 15; }

    public static @NotNull MapTile empty(int chunkX, int chunkZ) {
        byte[] terrain = new byte[COLUMNS];
        Arrays.fill(terrain, MapTerrain.VOID.id());
        return new MapTile(chunkX, chunkZ, new short[COLUMNS], new short[COLUMNS], terrain, new byte[COLUMNS]);
    }

    public long chunk() { return ChunkPos.asLong(chunkX, chunkZ); }

    public boolean isEmpty() {
        for (int i = 0; i < COLUMNS; i++) {
            if (terrain[i] != MapTerrain.VOID.id()) return false;
        }
        return true;
    }

    public int groundAt(int x, int z) { return ground[index(x, z)]; }

    public int surfaceAt(int x, int z) { return surface[index(x, z)]; }

    public @NotNull MapTerrain terrainAt(int x, int z) { return MapTerrain.byId(terrain[index(x, z)]); }

    public boolean hasFlag(int x, int z, int flag) { return (flags[index(x, z)] & flag) != 0; }

    public int hash() {
        int hash = 31 * chunkX + chunkZ;
        hash = 31 * hash + Arrays.hashCode(ground);
        hash = 31 * hash + Arrays.hashCode(surface);
        hash = 31 * hash + Arrays.hashCode(terrain);
        return 31 * hash + Arrays.hashCode(flags);
    }

    public boolean sameAs(@NotNull MapTile other) {
        return chunkX == other.chunkX && chunkZ == other.chunkZ && Arrays.equals(ground, other.ground)
                && Arrays.equals(surface, other.surface) && Arrays.equals(terrain, other.terrain)
                && Arrays.equals(flags, other.flags);
    }

    //region Codec [编解码]
    private static void write(ByteBuf buffer, MapTile tile) {
        ByteBufCodecs.VAR_INT.encode(buffer, tile.chunkX);
        ByteBufCodecs.VAR_INT.encode(buffer, tile.chunkZ);
        if (tile.isEmpty()) {
            buffer.writeByte(EMPTY);
            return;
        }
        buffer.writeByte(FULL);
        for (int i = 0; i < COLUMNS; i++) {
            buffer.writeShort(tile.ground[i]);
            buffer.writeShort(tile.surface[i]);
            buffer.writeByte(tile.terrain[i]);
            buffer.writeByte(tile.flags[i]);
        }
    }

    private static MapTile read(ByteBuf buffer) {
        int chunkX = ByteBufCodecs.VAR_INT.decode(buffer);
        int chunkZ = ByteBufCodecs.VAR_INT.decode(buffer);
        byte kind = buffer.readByte();
        if (kind == EMPTY) return empty(chunkX, chunkZ);
        if (kind != FULL) throw new IllegalArgumentException("unknown map tile kind " + kind);

        short[] ground = new short[COLUMNS];
        short[] surface = new short[COLUMNS];
        byte[] terrain = new byte[COLUMNS];
        byte[] flags = new byte[COLUMNS];
        for (int i = 0; i < COLUMNS; i++) {
            ground[i] = buffer.readShort();
            surface[i] = buffer.readShort();
            terrain[i] = buffer.readByte();
            flags[i] = buffer.readByte();
        }
        return new MapTile(chunkX, chunkZ, ground, surface, terrain, flags);
    }
    //endregion
}
