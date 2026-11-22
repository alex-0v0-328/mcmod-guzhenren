package net.alex.guzhenren.gameplay.blissful.territory;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * 福地[Blissful Land]的疆域：以原点区块为中心、边长 {@code 2 * half} 区块的正方形，减去四侧各自割弃的条带宽度。
 * <ul>
 *   <li>区块坐标范围为 {@code -half .. half - 1}（边长为偶数，原点区块在中心偏东南一格）；割弃宽度按区块计，
 *       {@link #minChunkX} 等四个边界由宽度推出，不另存。</li>
 *   <li>中心 {@code 2 * core} 见方的区块不可割弃：{@link #maxCede} 给出某侧还能割多少，宽度不设别的上限。</li>
 *   <li>{@link #isCeded} 只对原正方形内、现疆域外的区块为真；原正方形外本就是虚空，由生成器负责，不算割弃。</li>
 *   <li>一区块 = 4 亩，{@link #acres} 用 long 避免溢出。</li>
 * </ul>
 */

public record Territory(int half, int core, int north, int east, int south, int west) {

    public static final int ACRES_PER_CHUNK = 4;

    public enum Side { NORTH, EAST, SOUTH, WEST }

    public static @NotNull Territory whole(int half, int core) { return new Territory(half, core, 0, 0, 0, 0); }

    public int minChunkX() { return -half + west; }

    public int maxChunkX() { return half - 1 - east; }

    public int minChunkZ() { return -half + north; }

    public int maxChunkZ() { return half - 1 - south; }

    public boolean contains(int chunkX, int chunkZ) {
        return chunkX >= minChunkX() && chunkX <= maxChunkX() && chunkZ >= minChunkZ() && chunkZ <= maxChunkZ();
    }

    public boolean isCeded(int chunkX, int chunkZ) {
        boolean original = chunkX >= -half && chunkX < half && chunkZ >= -half && chunkZ < half;
        return original && !contains(chunkX, chunkZ);
    }

    public int width(@NotNull Side side) {
        return switch (side) {
            case NORTH -> north;
            case EAST -> east;
            case SOUTH -> south;
            case WEST -> west;
        };
    }

    public int maxCede(@NotNull Side side) { return half - core - width(side); }

    public boolean canCede(@NotNull Side side, int width) { return width >= 1 && width <= maxCede(side); }

    public @NotNull Territory cede(@NotNull Side side, int width) {
        if (!canCede(side, width)) throw new IllegalArgumentException("cannot cede " + width + " chunks " + side);
        return switch (side) {
            case NORTH -> new Territory(half, core, north + width, east, south, west);
            case EAST -> new Territory(half, core, north, east + width, south, west);
            case SOUTH -> new Territory(half, core, north, east, south + width, west);
            case WEST -> new Territory(half, core, north, east, south, west + width);
        };
    }

    public long chunks() { return (long) (maxChunkX() - minChunkX() + 1) * (maxChunkZ() - minChunkZ() + 1); }

    public long acres() { return chunks() * ACRES_PER_CHUNK; }

    public void save(@NotNull CompoundTag tag) {
        tag.putInt("north", north);
        tag.putInt("east", east);
        tag.putInt("south", south);
        tag.putInt("west", west);
    }

    public static @NotNull Territory load(@NotNull CompoundTag tag, int half, int core) {
        return new Territory(half, core, tag.getInt("north"), tag.getInt("east"), tag.getInt("south"),
                tag.getInt("west"));
    }
}
