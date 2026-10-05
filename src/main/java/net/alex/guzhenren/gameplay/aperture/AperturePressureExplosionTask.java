package net.alex.guzhenren.gameplay.aperture;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The block side of the ten-extreme pressure explosion [空窍压力爆炸]: the crater is carved over
 * several server ticks instead of one, so the largest blast (Great Strength True Martial [大力真武体],
 * radius 112) does not freeze the world. Interior blocks go out silently ({@code UPDATE_CLIENTS}
 * only, no drops); one deferred neighbor pass over the rim [球壳边缘] keeps sand, fluids and torches
 * from floating. The shell [壳] walks center-outwards and each column's radius carries a per-explosion
 * jitter [噪声扰动]. The crater floor is physique-specific: layered ice [分层冰] for Northern Dark
 * Ice Soul, magma and lava for Blazing Glory, scattered gold for Myriad Gold; Northern Dark Ice Soul
 * also converts the surface soil of an outer ring [雪化环带] to snow. Entity damage, self-damage and
 * the pressure reset stay in {@link AperturePressureService#detonate} -- they resolve at tick zero.
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureService#detonate(net.minecraft.server.level.ServerPlayer)
 * @since 1.0.0
 */

public final class AperturePressureExplosionTask {

    private AperturePressureExplosionTask(@NotNull ServerLevel level, double x, double y, double z, int radius,
                                  @NotNull ExtremePhysique physique) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.seed = level.random.nextLong();
        this.physique = physique;
    }

    private static final List<AperturePressureExplosionTask> ACTIVE = new ArrayList<>();
    private static final int BLOCK_BUDGET = 65_536;
    private static final double NOISE_FLOOR = 0.9D;
    private static final double NOISE_RANGE = 0.1D;
    private static final int RING_MIN = 8;
    private static final double RING_NOISE_RANGE = 8.0D;
    private static final double LAVA_SHARE = 0.025D;
    private static final double GOLD_SHARE = 0.0125D;
    private static final double POWDER_SNOW_SHARE = 0.05D;
    private static final int BLUE_ICE_THRESHOLD = -32;
    private static final int GOLD_ORE_THRESHOLD = 32;
    private final ServerLevel level;
    private final double x;
    private final double y;
    private final double z;
    private final int radius;
    private final long seed;
    private final ExtremePhysique physique;
    private final LongArrayList shell = new LongArrayList();
    private final LongArrayList rim = new LongArrayList();
    private int shellDistance;
    private int shellIndex;
    private int floorCursor;
    private int ringCursor;
    private int rimIndex;
    private Phase phase = Phase.CLEAR;

    private enum Phase { CLEAR, FLOOR, RING, RIM }

    public static void start(@NotNull ServerLevel level, double x, double y, double z, int radius,
                             @NotNull ExtremePhysique physique) {
        ACTIVE.add(new AperturePressureExplosionTask(level, x, y, z, radius, physique));
    }

    public static void tickAll() { ACTIVE.removeIf(AperturePressureExplosionTask::tick); }

    public static void clear() { ACTIVE.clear(); }

    static double getColumnJitter(int blockX, int blockZ, long seed) {
        long hash = blockX * 0x9E3779B97F4A7C15L ^ blockZ * 0xC2B2AE3D27D4EB4FL ^ seed;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash & 0xFFFFFL) * (NOISE_RANGE / 0x10_0000L);
    }

    static double getColumnRadius(int radius, int blockX, int blockZ, long seed) {
        return radius * (NOISE_FLOOR + getColumnJitter(blockX, blockZ, seed));
    }

    static int getColumnFloorY(double centerY, double columnRadius, double horizontalSquared) {
        return (int) Math.ceil(centerY - Math.sqrt(columnRadius * columnRadius - horizontalSquared) - 0.5D) - 1;
    }

    static int getIceTier(int floorY) {
        if (floorY < BLUE_ICE_THRESHOLD) return 2;
        return floorY < 0 ? 1 : 0;
    }

    static int getGoldTier(int floorY) {
        if (floorY < 0) return 0;
        return floorY < GOLD_ORE_THRESHOLD ? 1 : 2;
    }

    static boolean isLavaColumn(int blockX, int blockZ, long seed) {
        return getColumnJitter(blockX, blockZ, seed ^ 0x5DEECE66DL) < LAVA_SHARE;
    }

    static boolean isGoldColumn(int blockX, int blockZ, long seed) {
        return getColumnJitter(blockX, blockZ, seed ^ 0x2545F4914F6CDD1DL) < GOLD_SHARE;
    }

    static boolean isPowderSnowColumn(int blockX, int blockZ, long seed) {
        return getColumnJitter(blockX, blockZ, seed ^ 0x9E3779B9L) >= POWDER_SNOW_SHARE;
    }

    static double ringOuterRadius(int radius, int blockX, int blockZ, long seed) {
        return radius + RING_MIN
                + getColumnJitter(blockX, blockZ, seed ^ 0xC2B2AE3DL) / NOISE_RANGE * RING_NOISE_RANGE;
    }

    private boolean tick() {
        if (phase == Phase.CLEAR && tickClear()) {
            phase = hasFloor() ? Phase.FLOOR : getNextAfterFloor();
        }
        if (phase == Phase.FLOOR && tickFloor()) phase = getNextAfterFloor();
        if (phase == Phase.RING && tickRing()) phase = Phase.RIM;
        return phase == Phase.RIM && tickRim();
    }

    private Phase getNextAfterFloor() {
        return physique == ExtremePhysique.NORTHERN_DARK_ICE_SOUL ? Phase.RING : Phase.RIM;
    }

    private boolean hasFloor() {
        return switch (physique) {
            case NORTHERN_DARK_ICE_SOUL, BLAZING_GLORY_LIGHTNING_BRILLIANCE, MYRIAD_GOLD_WONDROUS_ESSENCE -> true;
            default -> false;
        };
    }

    private boolean tickClear() {
        int budget = BLOCK_BUDGET;
        while (budget > 0) {
            if (shellIndex >= shell.size()) {
                if (shellDistance > radius) return true;
                fillShell();
                shellIndex = 0;
                continue;
            }
            clearOne(BlockPos.of(shell.getLong(shellIndex++)));
            budget--;
        }
        return false;
    }

    private boolean tickFloor() {
        int budget = BLOCK_BUDGET;
        int side = 2 * radius + 1;
        while (budget > 0 && floorCursor < side * side) {
            int i = floorCursor++;
            int blockX = Mth.floor(x) - radius + i % side;
            int blockZ = Mth.floor(z) - radius + i / side;
            double localRadius = getColumnRadius(radius, blockX, blockZ, seed);
            double dx = blockX + 0.5D - x;
            double dz = blockZ + 0.5D - z;
            double horizontalSquared = dx * dx + dz * dz;
            if (horizontalSquared > localRadius * localRadius) continue;
            int floorY = getColumnFloorY(y, localRadius, horizontalSquared);
            if (floorY < level.getMinBuildHeight() || floorY >= level.getMaxBuildHeight()) continue;
            BlockPos floorPos = new BlockPos(blockX, floorY, blockZ);
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(floorPos.getX()),
                    SectionPos.blockToSectionCoord(floorPos.getZ())) || level.getBlockState(floorPos).isAir()) {
                continue;
            }
            Block block = getFloorBlockAt(blockX, floorY, blockZ);
            if (block == null) continue;
            level.setBlock(floorPos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
            budget--;
        }
        return floorCursor >= side * side;
    }

    private boolean tickRing() {
        int budget = BLOCK_BUDGET;
        int outer = radius + RING_MIN + (int) Math.ceil(RING_NOISE_RANGE);
        int side = 2 * outer + 1;
        while (budget > 0 && ringCursor < side * side) {
            int i = ringCursor++;
            int blockX = Mth.floor(x) - outer + i % side;
            int blockZ = Mth.floor(z) - outer + i / side;
            double dx = blockX + 0.5D - x;
            double dz = blockZ + 0.5D - z;
            double horizontalSquared = dx * dx + dz * dz;
            double localRadius = getColumnRadius(radius, blockX, blockZ, seed);
            if (horizontalSquared <= localRadius * localRadius) continue;
            double ringOuter = ringOuterRadius(radius, blockX, blockZ, seed);
            if (horizontalSquared >= ringOuter * ringOuter) continue;
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(blockX),
                    SectionPos.blockToSectionCoord(blockZ))) continue;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ) - 1;
            if (surfaceY < level.getMinBuildHeight()) continue;
            BlockPos surfacePos = new BlockPos(blockX, surfaceY, blockZ);
            if (!isSoilSurface(level.getBlockState(surfacePos))) continue;
            Block snow = isPowderSnowColumn(blockX, blockZ, seed) ? Blocks.POWDER_SNOW : Blocks.SNOW_BLOCK;
            level.setBlock(surfacePos, snow.defaultBlockState(), Block.UPDATE_CLIENTS);
            rim.add(surfacePos.asLong());
            budget--;
        }
        return ringCursor >= side * side;
    }

    private boolean tickRim() {
        int budget = BLOCK_BUDGET;
        while (budget > 0 && rimIndex < rim.size()) {
            level.updateNeighborsAt(BlockPos.of(rim.getLong(rimIndex++)), Blocks.AIR);
            budget--;
        }
        return rimIndex >= rim.size();
    }

    private @Nullable Block getFloorBlockAt(int blockX, int floorY, int blockZ) {
        return switch (physique) {
            case NORTHERN_DARK_ICE_SOUL -> switch (getIceTier(floorY)) {
                case 2 -> Blocks.BLUE_ICE;
                case 1 -> Blocks.PACKED_ICE;
                default -> Blocks.ICE;
            };
            case BLAZING_GLORY_LIGHTNING_BRILLIANCE ->
                    isLavaColumn(blockX, blockZ, seed) ? Blocks.LAVA : Blocks.MAGMA_BLOCK;
            case MYRIAD_GOLD_WONDROUS_ESSENCE -> {
                if (!isGoldColumn(blockX, blockZ, seed)) yield null;
                yield switch (getGoldTier(floorY)) {
                    case 0 -> Blocks.DEEPSLATE_GOLD_ORE;
                    case 1 -> Blocks.GOLD_ORE;
                    default -> Blocks.GOLD_BLOCK;
                };
            }
            default -> null;
        };
    }

    private boolean isSoilSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT);
    }

    private void fillShell() {
        shell.clear();
        int distance = shellDistance++;
        int centerX = Mth.floor(x);
        int centerY = Mth.floor(y);
        int centerZ = Mth.floor(z);
        if (distance == 0) {
            addInside(centerX, centerY, centerZ);
            return;
        }
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                addInside(centerX + dx, centerY + dy, centerZ - distance);
                addInside(centerX + dx, centerY + dy, centerZ + distance);
            }
        }
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dz = 1 - distance; dz <= distance - 1; dz++) {
                addInside(centerX + dx, centerY - distance, centerZ + dz);
                addInside(centerX + dx, centerY + distance, centerZ + dz);
            }
        }
        for (int dy = 1 - distance; dy <= distance - 1; dy++) {
            for (int dz = 1 - distance; dz <= distance - 1; dz++) {
                addInside(centerX - distance, centerY + dy, centerZ + dz);
                addInside(centerX + distance, centerY + dy, centerZ + dz);
            }
        }
    }

    private void addInside(int blockX, int blockY, int blockZ) {
        if (blockY < level.getMinBuildHeight() || blockY >= level.getMaxBuildHeight()) return;
        if (!isOutsideSphere(blockX, blockY, blockZ)) shell.add(BlockPos.asLong(blockX, blockY, blockZ));
    }

    private void clearOne(BlockPos pos) {
        if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()))) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (isRim(pos)) rim.add(pos.asLong());
    }

    private boolean isRim(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (neighbor.getY() >= level.getMinBuildHeight() && neighbor.getY() < level.getMaxBuildHeight()
                    && isOutsideSphere(neighbor.getX(), neighbor.getY(), neighbor.getZ())) return true;
        }
        return false;
    }

    private boolean isOutsideSphere(int blockX, int blockY, int blockZ) {
        double dx = blockX + 0.5D - x;
        double dy = blockY + 0.5D - y;
        double dz = blockZ + 0.5D - z;
        double localRadius = getColumnRadius(radius, blockX, blockZ, seed);
        return dx * dx + dy * dy + dz * dz > localRadius * localRadius;
    }
}
