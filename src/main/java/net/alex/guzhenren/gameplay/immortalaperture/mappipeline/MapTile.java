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
 *   <li>六个数组都是 {@value #COLUMNS} 列，列下标 = {@code (z & 15) * 16 + (x & 15)}（{@link #index}）；
 *       {@code ground} 是真实地面 y、{@code surface} 是最高非空气方块 y、{@code terrain} 是表面的 {@link MapTerrain} 序号、
 *       {@code flags} 是 {@link #FLAG_FLUID}、{@link #FLAG_BLOCK_ENTITY}、{@link #FLAG_TRUNK} 的位或；方块实体只认该列
 *       地面下 8 格到上 32 格内的（{@link MapScanner#BLOCK_ENTITY_BELOW}、{@link MapScanner#BLOCK_ENTITY_ABOVE}）。</li>
 *   <li>树冠：{@code low} 是顶上一段树冠的最低 y（悬空时高于 {@code ground + 1}，其余列恒为 {@code ground + 1}）；
 *       {@link #FLAG_TRUNK} 表示树冠下从地面上一格到 {@code low - 1} 全是原木（树干）；{@code under} 是地面方块的地貌，
 *       树冠悬空时客户端拿它画树下的地。六参构造器给「没有悬空树冠」的片（测试与旧调用方用）。</li>
 *   <li>⚠ 数组按引用持有、不拷贝，造出来后谁都不许改；记录的 {@code equals} 只比引用，比内容用 {@link #sameAs}，
 *       {@link #hash} 是内容哈希（服务端拿它判「变没变」）。</li>
 *   <li>{@link #STREAM_CODEC}：区块坐标两个 varint，再按列写 {@code ground}、{@code surface}、{@code low}（各 2 字节）、
 *       {@code terrain}、{@code under}、{@code flags}（各 1 字节）；整片 {@link MapTerrain#VOID} 的只写坐标加一个标记
 *       字节，满片约 2.3 KB，每包 32 片约 72 KB；标记字节不认识的抛异常（坏包）。</li>
 * </ul>
 */

public record MapTile(int chunkX, int chunkZ, short[] ground, short[] surface, byte[] terrain, byte[] flags,
                      short[] low, byte[] under) {

    public static final int COLUMNS = 256;
    public static final int FLAG_FLUID = 1;
    public static final int FLAG_BLOCK_ENTITY = 2;
    public static final int FLAG_TRUNK = 4;
    private static final byte FULL = 0;
    private static final byte EMPTY = 1;
    public static final StreamCodec<ByteBuf, MapTile> STREAM_CODEC = StreamCodec.of(MapTile::write, MapTile::read);

    public MapTile {
        if (ground.length != COLUMNS || surface.length != COLUMNS || terrain.length != COLUMNS
                || flags.length != COLUMNS || low.length != COLUMNS || under.length != COLUMNS) {
            throw new IllegalArgumentException("a map tile has " + COLUMNS + " columns");
        }
    }

    public MapTile(int chunkX, int chunkZ, short[] ground, short[] surface, byte[] terrain, byte[] flags) {
        this(chunkX, chunkZ, ground, surface, terrain, flags, resting(ground), terrain.clone());
    }

    private static short[] resting(short[] ground) {
        short[] low = new short[ground.length];
        for (int i = 0; i < ground.length; i++) low[i] = (short) (ground[i] + 1);
        return low;
    }

    public static int index(int x, int z) { return (z & 15) << 4 | x & 15; }

    public static @NotNull MapTile empty(int chunkX, int chunkZ) {
        byte[] terrain = new byte[COLUMNS];
        Arrays.fill(terrain, MapTerrain.VOID.id());
        return new MapTile(chunkX, chunkZ, new short[COLUMNS], new short[COLUMNS], terrain, new byte[COLUMNS],
                new short[COLUMNS], terrain.clone());
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

    public int lowAt(int x, int z) { return low[index(x, z)]; }

    public @NotNull MapTerrain terrainAt(int x, int z) { return MapTerrain.byId(terrain[index(x, z)]); }

    public @NotNull MapTerrain underAt(int x, int z) { return MapTerrain.byId(under[index(x, z)]); }

    public boolean hasFlag(int x, int z, int flag) { return (flags[index(x, z)] & flag) != 0; }

    public int hash() {
        int hash = 31 * chunkX + chunkZ;
        hash = 31 * hash + Arrays.hashCode(ground);
        hash = 31 * hash + Arrays.hashCode(surface);
        hash = 31 * hash + Arrays.hashCode(terrain);
        hash = 31 * hash + Arrays.hashCode(flags);
        hash = 31 * hash + Arrays.hashCode(low);
        return 31 * hash + Arrays.hashCode(under);
    }

    public boolean sameAs(@NotNull MapTile other) {
        return chunkX == other.chunkX && chunkZ == other.chunkZ && Arrays.equals(ground, other.ground)
                && Arrays.equals(surface, other.surface) && Arrays.equals(terrain, other.terrain)
                && Arrays.equals(flags, other.flags) && Arrays.equals(low, other.low)
                && Arrays.equals(under, other.under);
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
            buffer.writeShort(tile.low[i]);
            buffer.writeByte(tile.terrain[i]);
            buffer.writeByte(tile.under[i]);
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
        short[] low = new short[COLUMNS];
        byte[] terrain = new byte[COLUMNS];
        byte[] under = new byte[COLUMNS];
        byte[] flags = new byte[COLUMNS];
        for (int i = 0; i < COLUMNS; i++) {
            ground[i] = buffer.readShort();
            surface[i] = buffer.readShort();
            low[i] = buffer.readShort();
            terrain[i] = buffer.readByte();
            under[i] = buffer.readByte();
            flags[i] = buffer.readByte();
        }
        return new MapTile(chunkX, chunkZ, ground, surface, terrain, flags, low, under);
    }
    //endregion
}
