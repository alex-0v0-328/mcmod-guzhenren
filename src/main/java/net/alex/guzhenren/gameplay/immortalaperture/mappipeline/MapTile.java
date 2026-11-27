package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 一个区块的地表摘要（地图片[Map Tile]）：服务端 {@link MapScanner} 扫出，经地图片载荷下发，客户端地图按它画。
 * <ul>
 *   <li>按列（{@value #COLUMNS} 列，下标 = {@code (z & 15) * 16 + (x & 15)}，{@link #index}）：{@code ground} 是真实地面 y
 *       （开垦、圈地选址、等高线与悬停读数用，从上往下跳过 {@link LandRules#isPassable}，先碰到流体就是流体那一格），
 *       {@code flags} 是 {@link #FLAG_FLUID}、{@link #FLAG_BLOCK_ENTITY} 的位或；方块实体只认该列地面下 8 格到上 32 格内的
 *       （{@link MapScanner#BLOCK_ENTITY_BELOW}、{@link MapScanner#BLOCK_ENTITY_ABOVE}）。</li>
 *   <li>段[Run]：每列自上而下至多 {@value #MAX_RUNS} 段真实方块，第 {@code i} 列占段号 {@code [offsets[i], offsets[i + 1])}，
 *       段号 0 起最高；每段记顶 y、底 y、顶面地貌。底为 {@link #OPEN} 的段往下一直实（扫描停在这段里），只会是一列的
 *       最后一段；没有段的列是虚空（整列空气、已割弃或疆域外），地图不画。</li>
 *   <li>⚠ 数组按引用持有、不拷贝，造出来后谁都不许改；记录的 {@code equals} 只比引用，比内容用 {@link #sameAs}，
 *       {@link #hash} 是内容哈希（服务端拿它判「变没变」）。新片用 {@link Builder} 造。</li>
 *   <li>{@link #STREAM_CODEC}：区块坐标两个 varint，再按列写 {@code ground}（2 字节）、{@code flags}、段数（各 1 字节），
 *       每段顶、底（各 2 字节）与地貌（1 字节）；寻常一列一两段，满片约 2.5–3.5 KB，单列段数封顶后每包 32 片至多约
 *       690 KB。没有任何段的片只写坐标加一个标记字节；标记字节不认识或段数超限的抛异常（坏包）。</li>
 * </ul>
 */

public record MapTile(int chunkX, int chunkZ, short[] ground, byte[] flags, short[] offsets, short[] tops,
                      short[] bottoms, byte[] terrains) {

    public static final int COLUMNS = 256;
    public static final int MAX_RUNS = 16;
    public static final int OPEN = Short.MIN_VALUE;
    public static final int FLAG_FLUID = 1;
    public static final int FLAG_BLOCK_ENTITY = 2;
    private static final byte FULL = 0;
    private static final byte EMPTY = 1;
    public static final StreamCodec<ByteBuf, MapTile> STREAM_CODEC = StreamCodec.of(MapTile::write, MapTile::read);

    public MapTile {
        if (ground.length != COLUMNS || flags.length != COLUMNS || offsets.length != COLUMNS + 1) {
            throw new IllegalArgumentException("a map tile has " + COLUMNS + " columns");
        }
        int runs = offsets[COLUMNS];
        if (offsets[0] != 0 || tops.length != runs || bottoms.length != runs || terrains.length != runs) {
            throw new IllegalArgumentException("map tile run arrays do not match the column offsets");
        }
        for (int i = 0; i < COLUMNS; i++) {
            int count = offsets[i + 1] - offsets[i];
            if (count < 0 || count > MAX_RUNS) throw new IllegalArgumentException("bad run count " + count);
        }
    }

    public static int index(int x, int z) { return (z & 15) << 4 | x & 15; }

    public static @NotNull MapTile empty(int chunkX, int chunkZ) {
        return new MapTile(chunkX, chunkZ, new short[COLUMNS], new byte[COLUMNS], new short[COLUMNS + 1],
                new short[0], new short[0], new byte[0]);
    }

    public long chunk() { return ChunkPos.asLong(chunkX, chunkZ); }

    public boolean isEmpty() { return tops.length == 0; }

    //region Columns [列]
    public int groundAt(int x, int z) { return ground[index(x, z)]; }

    public boolean hasFlag(int x, int z, int flag) { return (flags[index(x, z)] & flag) != 0; }

    public int runs(int index) { return offsets[index + 1] - offsets[index]; }

    /** 第 {@code index} 列从上往下第 {@code k} 段的段号。 */
    public int run(int index, int k) { return offsets[index] + k; }

    public int runTop(int run) { return tops[run]; }

    public int runBottom(int run) { return bottoms[run]; }

    public boolean isOpen(int run) { return bottoms[run] == OPEN; }

    public @NotNull MapTerrain runTerrain(int run) { return MapTerrain.byId(terrains[run]); }

    /** 这一列最高一段的顶面地貌，虚空列为 {@link MapTerrain#VOID}。 */
    public @NotNull MapTerrain terrainAt(int x, int z) {
        int index = index(x, z);
        return runs(index) == 0 ? MapTerrain.VOID : runTerrain(offsets[index]);
    }
    //endregion

    public int hash() {
        int hash = 31 * chunkX + chunkZ;
        hash = 31 * hash + Arrays.hashCode(ground);
        hash = 31 * hash + Arrays.hashCode(flags);
        hash = 31 * hash + Arrays.hashCode(offsets);
        hash = 31 * hash + Arrays.hashCode(tops);
        hash = 31 * hash + Arrays.hashCode(bottoms);
        return 31 * hash + Arrays.hashCode(terrains);
    }

    public boolean sameAs(@NotNull MapTile other) {
        return chunkX == other.chunkX && chunkZ == other.chunkZ && Arrays.equals(ground, other.ground)
                && Arrays.equals(flags, other.flags) && Arrays.equals(offsets, other.offsets)
                && Arrays.equals(tops, other.tops) && Arrays.equals(bottoms, other.bottoms)
                && Arrays.equals(terrains, other.terrains);
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
            buffer.writeByte(tile.flags[i]);
            buffer.writeByte(tile.runs(i));
            for (int run = tile.offsets[i]; run < tile.offsets[i + 1]; run++) {
                buffer.writeShort(tile.tops[run]);
                buffer.writeShort(tile.bottoms[run]);
                buffer.writeByte(tile.terrains[run]);
            }
        }
    }

    private static MapTile read(ByteBuf buffer) {
        int chunkX = ByteBufCodecs.VAR_INT.decode(buffer);
        int chunkZ = ByteBufCodecs.VAR_INT.decode(buffer);
        byte kind = buffer.readByte();
        if (kind == EMPTY) return empty(chunkX, chunkZ);
        if (kind != FULL) throw new IllegalArgumentException("unknown map tile kind " + kind);

        Builder builder = new Builder();
        for (int i = 0; i < COLUMNS; i++) {
            builder.ground(i, buffer.readShort()).flag(i, buffer.readByte());
            int count = buffer.readUnsignedByte();
            if (count > MAX_RUNS) throw new IllegalArgumentException("a map tile column has " + count + " runs");
            for (int k = 0; k < count; k++) {
                builder.run(i, buffer.readShort(), buffer.readShort(), MapTerrain.byId(buffer.readByte()));
            }
        }
        return builder.build(chunkX, chunkZ);
    }
    //endregion

    /** 按列攒段的地图片构造器：段按列从上往下依次 {@link #run}，列之间顺序不限；每列超过 {@value #MAX_RUNS} 段抛异常。 */
    public static final class Builder {

        private final short[] ground = new short[COLUMNS];
        private final byte[] flags = new byte[COLUMNS];
        private final short[][] columns = new short[COLUMNS][];
        private final byte[] counts = new byte[COLUMNS];

        public Builder ground(int index, int y) {
            ground[index] = (short) y;
            return this;
        }

        public Builder flag(int index, int flag) {
            flags[index] |= (byte) flag;
            return this;
        }

        public Builder run(int index, int top, int bottom, @NotNull MapTerrain terrain) {
            int count = counts[index];
            if (count >= MAX_RUNS) throw new IllegalStateException("a map tile column holds " + MAX_RUNS + " runs");
            short[] runs = columns[index];
            if (runs == null) {
                runs = new short[6];
            } else if (runs.length < count * 3 + 3) {
                runs = Arrays.copyOf(runs, runs.length * 2);
            }
            runs[count * 3] = (short) top;
            runs[count * 3 + 1] = (short) bottom;
            runs[count * 3 + 2] = terrain.id();
            columns[index] = runs;
            counts[index]++;
            return this;
        }

        public int runs(int index) { return counts[index]; }

        public int groundOf(int index) { return ground[index]; }

        public @NotNull MapTile build(int chunkX, int chunkZ) {
            short[] offsets = new short[COLUMNS + 1];
            for (int i = 0; i < COLUMNS; i++) offsets[i + 1] = (short) (offsets[i] + counts[i]);
            int total = offsets[COLUMNS];
            short[] tops = new short[total];
            short[] bottoms = new short[total];
            byte[] terrains = new byte[total];
            for (int i = 0; i < COLUMNS; i++) {
                for (int k = 0; k < counts[i]; k++) {
                    int run = offsets[i] + k;
                    tops[run] = columns[i][k * 3];
                    bottoms[run] = columns[i][k * 3 + 1];
                    terrains[run] = (byte) columns[i][k * 3 + 2];
                }
            }
            return new MapTile(chunkX, chunkZ, ground.clone(), flags.clone(), offsets, tops, bottoms, terrains);
        }
    }
}
