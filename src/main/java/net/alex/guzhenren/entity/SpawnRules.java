package net.alex.guzhenren.entity;

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
 * Where this mod's wild entities may spawn naturally: the placement checks {@link EntityRegistrationEvents} registers.
 *
 * <p>Flying Gu families spawn at or above the dimension's chunk-generator sea level -- NOT {@code canSeeSky},
 * because leaves count as cover and would empty every forest floor ({@code Level#getSeaLevel} is a deprecated
 * constant 63) -- and never in or over a fluid: the first non-air block at or below the spawn point must hold none.
 * Biomes are 3D and land ones reach over water (the Eastern Sea's shallows, swamps, lakes), so the land biome tags
 * alone would let them spawn on the water surface. Wild beasts spawn on animal-spawnable ground ({@code BlockTags.ANIMALS_SPAWNABLE_ON} underfoot)
 * with raw brightness above {@value #ANIMAL_MIN_LIGHT}.
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
