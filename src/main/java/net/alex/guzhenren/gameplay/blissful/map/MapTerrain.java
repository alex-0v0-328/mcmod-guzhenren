package net.alex.guzhenren.gameplay.blissful.map;

import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

/**
 * 福地地图[Land Map]的地貌号：地图片每列表面方块归到一类，客户端按类上色；序号即线上字节，只许在末尾追加。
 * <ul>
 *   <li>{@link #UNKNOWN} 只作客户端缺省值（迷雾），服务端从不发；{@link #VOID} 是已割弃或疆域外的整片。</li>
 *   <li>{@link #of} 先按方块与标签判：流体、荡魂山石与胆石、耕地（含黑土耕地）、作物（原版 {@link CropBlock} 按
 *       {@code isMaxAge} 分熟与未熟，其余 {@code #crops} 一律算未熟）、栅栏与栅栏门与墙、树叶、木头、土径、花、冰、
 *       雪、沙、沙砾、泥、草与草丛、泥土、石；都不中再用 {@link #ofMapColor} 按原版地图色归类，仍不中为
 *       {@link #OTHER}。</li>
 *   <li>⚠ 判定顺序有意义：树叶在花前（开花杜鹃树叶也在 {@code #flowers}），花在草丛前（花同在
 *       {@code #replaceable_by_trees}）。</li>
 * </ul>
 */

public enum MapTerrain {
    UNKNOWN, VOID, GRASS, FOLIAGE, WATER, SAND, STONE, SNOW, ICE, DIRT, GRAVEL, MUD,
    FARMLAND, CROP_GROWING, CROP_MATURE, FENCE, WOOD, PATH, FLOWER, LAVA, SOULSUNDER_STONE, GUTS_STONE, OTHER;

    private static final MapTerrain[] VALUES = values();

    public static @NotNull MapTerrain byId(int id) { return id >= 0 && id < VALUES.length ? VALUES[id] : UNKNOWN; }

    public byte id() { return (byte) ordinal(); }

    //region Classify [归类]
    public static @NotNull MapTerrain of(@NotNull BlockState state, @NotNull BlockGetter getter,
                                         @NotNull BlockPos pos) {
        FluidState fluid = state.getFluidState();
        if (state.is(Blocks.WATER) || state.is(Blocks.BUBBLE_COLUMN)) return WATER;
        if (state.is(Blocks.LAVA)) return LAVA;
        if (state.is(ModBlocks.SOULSUNDER_STONE)) return SOULSUNDER_STONE;
        if (state.is(ModBlocks.GUTS_STONE)) return GUTS_STONE;
        if (state.getBlock() instanceof FarmBlock) return FARMLAND;
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state) ? CROP_MATURE : CROP_GROWING;
        if (state.is(BlockTags.CROPS)) return CROP_GROWING;
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WALLS)) return FENCE;
        if (state.is(BlockTags.LEAVES)) return FOLIAGE;
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) return WOOD;
        if (state.is(Blocks.DIRT_PATH)) return PATH;
        if (state.is(BlockTags.FLOWERS)) return FLOWER;
        if (state.is(BlockTags.ICE)) return ICE;
        if (state.is(BlockTags.SNOW)) return SNOW;
        if (state.is(BlockTags.SAND)) return SAND;
        if (state.is(Blocks.GRAVEL)) return GRAVEL;
        if (state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS)) return MUD;
        if (state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.REPLACEABLE_BY_TREES)) return GRASS;
        if (state.is(BlockTags.DIRT)) return DIRT;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.TERRACOTTA)) return STONE;
        if (fluid.is(FluidTags.WATER)) return WATER;
        if (fluid.is(FluidTags.LAVA)) return LAVA;
        return ofMapColor(state.getMapColor(getter, pos));
    }

    public static @NotNull MapTerrain ofMapColor(@NotNull MapColor color) {
        if (color == MapColor.GRASS || color == MapColor.COLOR_LIGHT_GREEN) return GRASS;
        if (color == MapColor.PLANT || color == MapColor.COLOR_GREEN || color == MapColor.EMERALD) return FOLIAGE;
        if (color == MapColor.WATER) return WATER;
        if (color == MapColor.FIRE) return LAVA;
        if (color == MapColor.SAND) return SAND;
        if (color == MapColor.SNOW || color == MapColor.WOOL) return SNOW;
        if (color == MapColor.ICE) return ICE;
        if (color == MapColor.DIRT || color == MapColor.PODZOL || color == MapColor.CLAY) return DIRT;
        if (color == MapColor.WOOD || color == MapColor.COLOR_BROWN) return WOOD;
        if (color == MapColor.STONE || color == MapColor.DEEPSLATE || color == MapColor.COLOR_GRAY
                || color == MapColor.COLOR_LIGHT_GRAY || color.id >= MapColor.TERRACOTTA_WHITE.id
                && color.id <= MapColor.TERRACOTTA_BLACK.id) {
            return STONE;
        }
        return OTHER;
    }
    //endregion
}
