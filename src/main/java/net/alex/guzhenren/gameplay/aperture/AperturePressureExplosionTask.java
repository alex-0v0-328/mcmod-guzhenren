package net.alex.guzhenren.gameplay.aperture;

import com.google.common.base.Enums;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.alex.guzhenren.gameplay.aperture.AperturePressureExplosions.TickBudget;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
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
 * many server ticks instead of one, under the per-tick time budget every active blast shares
 * ({@link AperturePressureExplosions}), so the largest blast (Great Strength True Martial [大力真武体], radius 112,
 * some 5.9 million positions) opens as fast as the server allows, tens of seconds, without dropping the tick
 * rate. A blast starts carving only once every chunk it touches is loaded, and holds them loaded until it is done
 * ({@link CraterChunkTicket}), so a crater finishes with no player near. A cleared block a full block inside the
 * smallest column radius ({@code CraterShape.getMinColumnRadius}) skips the rim test: none of its neighbors can lie
 * outside. Interior blocks go out silently ({@code UPDATE_CLIENTS} only, no drops); one deferred neighbor pass over
 * the rim [球壳边缘] keeps sand, fluids and torches from floating; unbreakable blocks (bedrock, barriers, portal
 * frames) stay, so a blast at the bottom of the world never opens the void. The shell [壳] walks center-outwards and
 * each column's radius carries a per-explosion jitter [噪声扰动], read from {@link CraterShape}. The crater floor is
 * physique-specific: layered ice [分层冰] for Northern Dark Ice Soul, magma and lava for Blazing Glory, scattered
 * gold for Myriad Gold; Northern Dark Ice Soul also converts the surface soil of an outer ring [雪化环带] to snow.
 * Entity damage, self-damage and the pressure reset stay in {@link AperturePressureService#detonate} -- they
 * resolve at tick zero.
 *
 * <p>A blast survives a server stop: {@link #save} writes its center, radius, seed, physique, phase, every cursor
 * and the rim list into its level's {@link AperturePressureExplosions}; the level is the blast's dimension.
 * {@link #load} rebuilds the shell in progress instead of reading it -- the shell is a pure function of the center,
 * radius, seed and build height, and {@code shellDistance} already counts the shell being walked -- so the restored
 * blast carves exactly what the stopped one had left. The rim list is saved whole: which rim blocks were cleared,
 * not air or unbreakable already, cannot be told from the world afterwards.
 *
 * <p>Past 300 lines on purpose: the save format writes and reads every cursor the four phases advance, so it stays
 * beside them.
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureService#detonate(net.minecraft.server.level.ServerPlayer)
 * @since 1.0.0
 */

public final class AperturePressureExplosionTask {

    private AperturePressureExplosionTask(@NotNull ServerLevel level, double x, double y, double z, int radius,
                                  long seed, @NotNull ExtremePhysique physique) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.seed = seed;
        this.physique = physique;
        double inner = Math.max(0.0D, CraterShape.getMinColumnRadius(radius) - 1.0D);
        this.innerSquared = inner * inner;
        int reach = (getNextAfterFloor() == Phase.RING ? CraterShape.getRingReach(radius) : radius) + 1;
        this.chunks = new CraterChunkTicket(level, Mth.floor(x), Mth.floor(z), reach, seed);
    }

    private static final Direction[] DIRECTIONS = Direction.values();
    private final ServerLevel level;
    private final double x;
    private final double y;
    private final double z;
    private final int radius;
    private final long seed;
    private final ExtremePhysique physique;
    private final double innerSquared;
    private final CraterChunkTicket chunks;
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
        AperturePressureExplosions.get(level).add(
                new AperturePressureExplosionTask(level, x, y, z, radius, level.random.nextLong(), physique));
    }

    static @Nullable AperturePressureExplosionTask load(ServerLevel level, CompoundTag tag) {
        ExtremePhysique physique = ExtremePhysique.CODEC
                .parse(NbtOps.INSTANCE, StringTag.valueOf(tag.getString("physique"))).result().orElse(null);
        Phase phase = Enums.getIfPresent(Phase.class, tag.getString("phase")).orNull();
        int radius = tag.getInt("radius");
        if (physique == null || phase == null || radius < 0) return null;

        AperturePressureExplosionTask task = new AperturePressureExplosionTask(level, tag.getDouble("x"),
                tag.getDouble("y"), tag.getDouble("z"), radius, tag.getLong("seed"), physique);
        task.phase = phase;
        task.shellDistance = tag.getInt("shell_distance");
        if (phase == Phase.CLEAR && task.shellDistance > 0) {
            task.shellDistance--;
            task.fillShell();
        }
        task.shellIndex = tag.getInt("shell_index");
        task.floorCursor = tag.getInt("floor_cursor");
        task.ringCursor = tag.getInt("ring_cursor");
        task.rim.addElements(0, tag.getLongArray("rim"));
        task.rimIndex = tag.getInt("rim_index");
        return task;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putInt("radius", radius);
        tag.putLong("seed", seed);
        tag.putString("physique", physique.getSerializedName());
        tag.putString("phase", phase.name());
        tag.putInt("shell_distance", shellDistance);
        tag.putInt("shell_index", shellIndex);
        tag.putInt("floor_cursor", floorCursor);
        tag.putInt("ring_cursor", ringCursor);
        tag.putLongArray("rim", rim.toLongArray());
        tag.putInt("rim_index", rimIndex);
        return tag;
    }

    boolean tick(TickBudget budget) {
        if (!chunks.isLoaded()) return false;

        if (phase == Phase.CLEAR && tickClear(budget)) {
            phase = hasFloor() ? Phase.FLOOR : getNextAfterFloor();
        }
        if (phase == Phase.FLOOR && tickFloor(budget)) phase = getNextAfterFloor();
        if (phase == Phase.RING && tickRing(budget)) phase = Phase.RIM;
        if (phase != Phase.RIM || !tickRim(budget)) return false;

        chunks.release();
        return true;
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

    private boolean tickClear(TickBudget budget) {
        while (budget.hasTime()) {
            if (shellIndex >= shell.size()) {
                if (shellDistance > radius) return true;
                fillShell();
                shellIndex = 0;
                continue;
            }
            clearOne(BlockPos.of(shell.getLong(shellIndex++)));
        }
        return false;
    }

    private boolean tickFloor(TickBudget budget) {
        int side = 2 * radius + 1;
        while (floorCursor < side * side && budget.hasTime()) {
            int i = floorCursor++;
            int blockX = Mth.floor(x) - radius + i % side;
            int blockZ = Mth.floor(z) - radius + i / side;
            double localRadius = CraterShape.getColumnRadius(radius, blockX, blockZ, seed);
            double dx = blockX + 0.5D - x;
            double dz = blockZ + 0.5D - z;
            double horizontalSquared = dx * dx + dz * dz;
            if (horizontalSquared > localRadius * localRadius) continue;
            int floorY = CraterShape.getColumnFloorY(y, localRadius, horizontalSquared);
            if (floorY < level.getMinBuildHeight() || floorY >= level.getMaxBuildHeight()) continue;
            BlockPos floorPos = new BlockPos(blockX, floorY, blockZ);
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(floorPos.getX()),
                    SectionPos.blockToSectionCoord(floorPos.getZ())) || level.getBlockState(floorPos).isAir()) {
                continue;
            }
            Block block = getFloorBlockAt(blockX, floorY, blockZ);
            if (block == null) continue;
            level.setBlock(floorPos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return floorCursor >= side * side;
    }

    private boolean tickRing(TickBudget budget) {
        int outer = CraterShape.getRingReach(radius);
        int side = 2 * outer + 1;
        while (ringCursor < side * side && budget.hasTime()) {
            int i = ringCursor++;
            int blockX = Mth.floor(x) - outer + i % side;
            int blockZ = Mth.floor(z) - outer + i / side;
            double dx = blockX + 0.5D - x;
            double dz = blockZ + 0.5D - z;
            double horizontalSquared = dx * dx + dz * dz;
            double localRadius = CraterShape.getColumnRadius(radius, blockX, blockZ, seed);
            if (horizontalSquared <= localRadius * localRadius) continue;
            double ringOuter = CraterShape.getRingOuterRadius(radius, blockX, blockZ, seed);
            if (horizontalSquared >= ringOuter * ringOuter) continue;
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(blockX),
                    SectionPos.blockToSectionCoord(blockZ))) continue;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ) - 1;
            if (surfaceY < level.getMinBuildHeight()) continue;
            BlockPos surfacePos = new BlockPos(blockX, surfaceY, blockZ);
            if (!isSoilSurface(level.getBlockState(surfacePos))) continue;
            boolean powder = CraterShape.isPowderSnowColumn(blockX, blockZ, seed);
            Block snow = powder ? Blocks.POWDER_SNOW : Blocks.SNOW_BLOCK;
            level.setBlock(surfacePos, snow.defaultBlockState(), Block.UPDATE_CLIENTS);
            rim.add(surfacePos.asLong());
        }
        return ringCursor >= side * side;
    }

    private boolean tickRim(TickBudget budget) {
        while (rimIndex < rim.size() && budget.hasTime()) {
            level.updateNeighborsAt(BlockPos.of(rim.getLong(rimIndex++)), Blocks.AIR);
        }
        return rimIndex >= rim.size();
    }

    private @Nullable Block getFloorBlockAt(int blockX, int floorY, int blockZ) {
        return switch (physique) {
            case NORTHERN_DARK_ICE_SOUL -> switch (CraterShape.getIceTier(floorY)) {
                case 2 -> Blocks.BLUE_ICE;
                case 1 -> Blocks.PACKED_ICE;
                default -> Blocks.ICE;
            };
            case BLAZING_GLORY_LIGHTNING_BRILLIANCE ->
                    CraterShape.isLavaColumn(blockX, blockZ, seed) ? Blocks.LAVA : Blocks.MAGMA_BLOCK;
            case MYRIAD_GOLD_WONDROUS_ESSENCE -> {
                if (!CraterShape.isGoldColumn(blockX, blockZ, seed)) yield null;
                yield switch (CraterShape.getGoldTier(floorY)) {
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
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (isRim(pos)) rim.add(pos.asLong());
    }

    private boolean isRim(BlockPos pos) {
        double dx = pos.getX() + 0.5D - x;
        double dy = pos.getY() + 0.5D - y;
        double dz = pos.getZ() + 0.5D - z;
        if (dx * dx + dy * dy + dz * dz < innerSquared) return false;
        for (Direction direction : DIRECTIONS) {
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
        double localRadius = CraterShape.getColumnRadius(radius, blockX, blockZ, seed);
        return dx * dx + dy * dy + dz * dz > localRadius * localRadius;
    }
}
