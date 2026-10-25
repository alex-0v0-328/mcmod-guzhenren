package net.alex.guzhenren.gameplay.rewind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

/**
 * Whether a chunk is land or water to an As Before Gu [如故蛊]: the Mountain As Before Gu [山如故] restores
 * {@link #LAND}, the River As Before Gu [江如故] restores {@link #WATER}, and each refuses the other.
 *
 * <p>Decided by biome, read once at the chunk's middle column on its surface: a biome tagged {@code is_ocean},
 * {@code is_deep_ocean} or {@code is_river} makes the chunk water, anything else land -- so a lake in a plain is
 * land, and a Southern Border [南疆] biome ported with Terralith's tags sorts itself. One read per chunk, no
 * column count.
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkRewindService
 * @since 1.0.0
 */

public enum ChunkKind {

    LAND, WATER;

    public static @NotNull ChunkKind of(@NotNull ServerLevel level, @NotNull LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int x = pos.getMiddleBlockX();
        int z = pos.getMiddleBlockZ();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        boolean water = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)
                || biome.is(BiomeTags.IS_RIVER);
        return water ? WATER : LAND;
    }
}
