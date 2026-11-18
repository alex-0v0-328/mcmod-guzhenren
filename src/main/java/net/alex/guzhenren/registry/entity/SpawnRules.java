package net.alex.guzhenren.registry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 本模组野生实体自然生成位置的静态谓词检查（由 {@link EntityRegistrationEvents} 挂载）。
 * <ul>
 *   <li>飞行蛊家族要求生成高度不低于维度区块生成器的海平面，刻意不使用 {@code canSeeSky}（避免树叶遮挡清空整片森林）也
 *       不使用已弃用的固定海平面常量 63（{@code Level#getSeaLevel}）；同时向下探测至首个非空气方块，确保生成点不在流体
 *       内部或流体上方，防止三维陆地群系延伸到海面或湖面时将野生蛊刷在水上。</li>
 *   <li>全部野兽要求脚下方块符合 {@code BlockTags.ANIMALS_SPAWNABLE_ON} 且原始亮度严格高于
 *       {@value #ANIMAL_MIN_LIGHT}。</li>
 * </ul>
 */

public final class SpawnRules {

    private static final int ANIMAL_MIN_LIGHT = 8;

    private SpawnRules() {}

    static boolean onAnimalGround(ServerLevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
                && level.getRawBrightness(pos, 0) > ANIMAL_MIN_LIGHT;
    }

    static boolean onTheSurface(ServerLevelAccessor level, BlockPos pos) {
        return pos.getY() >= level.getLevel().getChunkSource().getGenerator().getSeaLevel()
                && !isInOrOverFluid(level, pos);
    }

    private static boolean isInOrOverFluid(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (cursor.getY() > level.getMinBuildHeight() && level.getBlockState(cursor).isAir()) {
            cursor.move(Direction.DOWN);
        }
        return !level.getFluidState(cursor).isEmpty();
    }
}
