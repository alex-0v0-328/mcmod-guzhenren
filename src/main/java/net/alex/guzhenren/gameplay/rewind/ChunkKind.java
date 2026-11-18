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
 * 如故系地形还原的区块水陆分类枚举：山如故[Mountain As Before Gu]仅还原 {@link #LAND}（陆地），
 * 江如故[River As Before Gu]仅还原 {@link #WATER}（水域），各自拒绝另一类；{@link #of} 仅采样区块中心列地表高度处的生物
 * 群系标签（含河流与深浅海洋标签为 {@code WATER}，因此平原内的小湖泊算作 {@code LAND}，而带 Terralith 标签移植的四域群
 * 系自动按各自群系标签归类）。
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
