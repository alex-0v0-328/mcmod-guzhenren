package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地形崩裂[Shatter]服务：负责范围伤害[Area Damage]拳击、蓄力一拳[Charged Punch]近战与终点破坏打击[Crash Strike]的前方方
 * 块破坏。
 * <ul>
 *   <li>本击力量点[Strength Points]达到 {@link #SOFT_POINTS} 时方可破块，可破坏方块硬度上限按力量点分三档
 *       （{@link #getMaxHardness}；终点破坏打击叠入两猪之力后直接调 {@link #shatterWith}）；带方块实体、
 *       流体或不可破坏的方块一律跳过。</li>
 *   <li>崩裂半径取对应打击半径的一半：范围伤害取视线同轴锥（{@link #getConeCells}）；蓄力一拳取从脚底平层到头顶上一格的
 *       水平扇形（{@link #getFanCells}，不挖脚下地面）；终点破坏打击取同高度圆柱圆盘（{@link #getDiscCells}，冲撞起点坑
 *       额外向下多挖一层地面）。所有候选格按距起点由近到远排序。</li>
 *   <li>{@link #shatter} 依序遍历候选格并返回实际破坏块数：先查 {@link ModEvents#canGrief}，再查视线遮挡
 *       （{@code isExposed}，近处方块先破后自然让出后续视线）与玩家破坏权限（{@link CrashBreach#mayBreak}），达到块数上
 *       限 {@code cap} 即停。</li>
 *   <li>按玩家空手挖掘规则结算（Alex 定）：移除方块并调用 {@code Block.destroy}，仅在非创造模式且空手可收获
 *       （{@code canHarvestBlock}）时调用 {@code playerDestroy} 产生掉落、经验与统计（需工具的石头、矿石等碎裂但不掉
 *       落）；整击若破除猪灵守护方块则统一激怒一次附近猪灵。</li>
 *   <li>⚠ 刻意不调每块都会广播粒子与音效的 {@code playerWillDestroy}（最多 256 块同时炸响会严重吵闹）：改为每
 *       {@link #PARTICLE_EVERY} 块发送一组碎屑粒子，整次崩裂仅在最近的首块位置播放一次低沉破坏音（{@link CrashBreach}
 *       沿路破块共用 {@link #showDebris} 与 {@link #playBreak}）。</li>
 *   <li>⚠ 双格高方块（门、高草、高花）在生存模式空手可收获时破任意半格即可正常掉落一份；而在创造模式或不可收获时破上半
 *       格，必须先由 {@link #clearLowerHalf} 以 {@code UPDATE_SUPPRESS_DROPS} 清掉下半格，否则下半格会在形状更新中额外
 *       爆出掉落物。</li>
 * </ul>
 */

public final class AreaDamageShatter {

    private AreaDamageShatter() {}

    public static final int SOFT_POINTS = 100;
    public static final float SOFT_HARDNESS = 0.6F;
    public static final int STONE_POINTS = 200;
    public static final float STONE_HARDNESS = 1.5F;
    public static final int HARD_POINTS = 400;
    public static final float HARD_HARDNESS = 3.0F;
    public static final float NONE = -1.0F;
    public static final int CONE_MAX_BLOCKS = 64;
    public static final int FAN_MAX_BLOCKS = 256;
    public static final int PARTICLE_EVERY = 4;
    private static final int PARTICLE_COUNT = 6;
    private static final double PARTICLE_SPREAD = 0.25D;
    private static final double PARTICLE_SPEED = 0.05D;
    private static final float SOUND_VOLUME = 1.5F;
    private static final float SOUND_PITCH = 0.6F;
    private static final double MIN_REACH = 0.5D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;
    private static final double ANGLE_SLACK = 1.0E-9D;

    //region Cells [格子]
    public static float getMaxHardness(int points) {
        if (points >= HARD_POINTS) return HARD_HARDNESS;
        if (points >= STONE_POINTS) return STONE_HARDNESS;
        return points >= SOFT_POINTS ? SOFT_HARDNESS : NONE;
    }

    public static boolean canShatter(int points) { return getMaxHardness(points) >= 0.0F; }

    public static boolean canBreak(float hardness, boolean hasBlockEntity, float maxHardness) {
        return !hasBlockEntity && hardness >= 0.0F && hardness <= maxHardness;
    }

    public static @NotNull List<BlockPos> getConeCells(@NotNull Vec3 origin, @NotNull Vec3 axis, double radius) {
        if (axis.lengthSqr() < MIN_LENGTH_SQR || !(radius > 0.0D)) return List.of();

        Vec3 unit = axis.normalize();
        List<BlockPos> cells = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(origin.subtract(radius, radius, radius)),
                BlockPos.containing(origin.add(radius, radius, radius)))) {
            if (AreaDamage.isInCone(origin, unit, radius, AreaDamage.HALF_ANGLE_DEGREES, pos.getCenter())) {
                cells.add(pos.immutable());
            }
        }
        return byDistance(cells, origin);
    }

    public static @NotNull List<BlockPos> getFanCells(@NotNull Vec3 feet, double height, @NotNull Vec3 axis,
                                                      double radius, double halfAngleDegrees, @NotNull Vec3 from) {
        if (axis.lengthSqr() < MIN_LENGTH_SQR) return List.of();
        return getLayerCells(feet, height, radius, 0, axis, Math.cos(Math.toRadians(halfAngleDegrees)), from);
    }

    public static @NotNull List<BlockPos> getDiscCells(@NotNull Vec3 feet, double height, double radius,
                                                       int floorLayers, @NotNull Vec3 from) {
        return getLayerCells(feet, height, radius, Math.max(0, floorLayers), null, 0.0D, from);
    }

    private static List<BlockPos> getLayerCells(Vec3 feet, double height, double radius, int floorLayers,
                                                @Nullable Vec3 axis, double cos, Vec3 from) {
        if (!(radius > 0.0D)) return List.of();

        int bottom = Mth.floor(feet.y + 0.5D) - floorLayers;
        int top = Mth.floor(feet.y + height) + 1;
        double minCos = cos - ANGLE_SLACK;
        List<BlockPos> cells = new ArrayList<>();
        for (int x = Mth.floor(feet.x - radius); x <= Mth.floor(feet.x + radius); x++) {
            for (int z = Mth.floor(feet.z - radius); z <= Mth.floor(feet.z + radius); z++) {
                double dx = x + 0.5D - feet.x;
                double dz = z + 0.5D - feet.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;
                if (axis != null && (distance < MIN_REACH || (dx * axis.x + dz * axis.z) / distance < minCos)) continue;
                for (int y = bottom; y <= top; y++) cells.add(new BlockPos(x, y, z));
            }
        }
        return byDistance(cells, from);
    }

    private static List<BlockPos> byDistance(List<BlockPos> cells, Vec3 from) {
        cells.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(from)));
        return cells;
    }
    //endregion

    //region Break [破坏]
    public static int shatter(@NotNull ServerPlayer player, @NotNull Vec3 from, @NotNull List<BlockPos> cells,
                              int points, int cap) {
        return shatterWith(player, from, cells, getMaxHardness(points), cap);
    }

    public static int shatterWith(@NotNull ServerPlayer player, @NotNull Vec3 from, @NotNull List<BlockPos> cells,
                                  float maxHardness, int cap) {
        ServerLevel level = player.serverLevel();
        if (maxHardness < 0.0F || player.isSpectator() || !ModEvents.canGrief(level, player)) return 0;

        BlockPos first = null;
        SoundType sound = null;
        boolean guarded = false;
        int broken = 0;
        for (BlockPos pos : cells) {
            if (broken >= cap) break;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof LiquidBlock) continue;
            if (!canBreak(state.getDestroySpeed(level, pos), state.hasBlockEntity(), maxHardness)) continue;
            if (!isExposed(player, level, from, pos) || !CrashBreach.mayBreak(player, level, pos, state)) continue;

            if (first == null) {
                first = pos;
                sound = state.getSoundType(level, pos, player);
            }
            guarded |= state.is(BlockTags.GUARDED_BY_PIGLINS);
            breakQuietly(player, level, pos, state, broken % PARTICLE_EVERY == 0);
            broken++;
        }
        if (first != null) playBreak(level, first, sound);
        if (guarded) PiglinAi.angerNearbyPiglins(player, false);
        return broken;
    }

    private static boolean isExposed(ServerPlayer player, ServerLevel level, Vec3 from, BlockPos pos) {
        BlockHitResult hit = level.clip(new ClipContext(from, pos.getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    static void showDebris(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D,
                pos.getY() + 0.5D, pos.getZ() + 0.5D, PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD,
                PARTICLE_SPREAD, PARTICLE_SPEED);
    }

    static void playBreak(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull SoundType sound) {
        level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, SOUND_VOLUME, SOUND_PITCH);
    }

    private static void breakQuietly(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state,
                                     boolean showParticles) {
        if (showParticles) showDebris(level, pos, state);
        boolean harvest = !player.isCreative() && state.canHarvestBlock(level, pos, player);
        if (!harvest) clearLowerHalf(level, pos, state);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_ALL);
        state.getBlock().destroy(level, pos, state);
        if (harvest) state.getBlock().playerDestroy(level, player, pos, state, null, ItemStack.EMPTY);
    }

    static void clearLowerHalf(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state) {
        EnumProperty<DoubleBlockHalf> half = BlockStateProperties.DOUBLE_BLOCK_HALF;
        if (!state.hasProperty(half) || state.getValue(half) != DoubleBlockHalf.UPPER) return;

        BlockPos below = pos.below();
        BlockState lower = level.getBlockState(below);
        if (lower.is(state.getBlock()) && lower.getValue(half) == DoubleBlockHalf.LOWER) {
            level.setBlock(below, lower.getFluidState().createLegacyBlock(),
                    Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }
    //endregion
}
