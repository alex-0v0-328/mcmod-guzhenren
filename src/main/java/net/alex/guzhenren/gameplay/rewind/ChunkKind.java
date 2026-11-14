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
 * 如故蛊[As Before Gu]眼中区块的陆地 / 水域分类。山如故[Mountain As Before Gu]还原 {@link #LAND}，
 * 江如故[River As Before Gu]还原 {@link #WATER}，各拒另一类。每区块只读中心列地表的群系，不数列，故平原湖算陆地，带
 * Terralith 标签移植的南疆群系自行归类。
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
