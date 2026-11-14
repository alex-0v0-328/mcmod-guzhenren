package net.alex.guzhenren.registry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 本模组野生实体的自然生成检查，由 {@link EntityRegistrationEvents} 注册。
 *
 * <ul>
 *   <li>飞行蛊家族在维度区块生成器海平面及以上生成，不用 {@code canSeeSky}（树叶算遮挡，会清空林地；
 *       {@code Level#getSeaLevel} 是弃用常量 63）；不在流体中或其上生成：生成点及以下首个非空气方块不得含流体，
 *       因群系是三维的，陆地群系伸到水面上（东海浅滩、沼泽、湖泊），只凭陆地群系标签会刷在水面。</li>
 *   <li>野兽在 {@code BlockTags.ANIMALS_SPAWNABLE_ON} 地面生成，原始亮度高于 {@value #ANIMAL_MIN_LIGHT}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see EntityRegistrationEvents
 * @since 1.0.0
 */

public final class SpawnRules {

    private static final int ANIMAL_MIN_LIGHT = 8;

    private SpawnRules() {}

    static <T extends Mob> boolean onAnimalGround(EntityType<T> type, ServerLevelAccessor level,
                                                  MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
                && level.getRawBrightness(pos, 0) > ANIMAL_MIN_LIGHT;
    }

    static <T extends Mob> boolean onTheSurface(EntityType<T> type, ServerLevelAccessor level,
                                                MobSpawnType reason, BlockPos pos, RandomSource random) {
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
