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

/**
 * 崩裂[Shatter]：范围冲击[Area Impact]拳击与蓄力一拳[Charged Punch]近战打碎身前地形。
 * <ul>
 *   <li>这一击的力量点[Strength Points] ≥ {@link #SOFT_POINTS} 才破，硬度上限按点数分三档（{@link #getMaxHardness}）；
 *       带方块实体、液体、不可破的一律不破。</li>
 *   <li>破坏区为伤害范围的一半：范围冲击是同轴锥（{@link #getConeCells}），蓄力一拳是同轴水平扇形，从脚面那层到头顶
 *       上一格（{@link #getFanCells}，不挖脚下地面）；格子按中心判定，由近到远。</li>
 *   <li>{@link #shatter} 按格子顺序逐格，返回破了几块：先问地形门槛 {@link ModEvents#canGrief}；眼睛到格中心被没破的
 *       方块挡住就跳过，近处先破，挖开的口子不挡后面；按玩家破坏问权限（{@link CrashBreach#mayBreak}）；破满上限
 *       即停。调用方先问 {@link #canShatter}，点数不够不必建格子。</li>
 *   <li>按玩家空手挖掘结算（Alex 定，不是静默置空）：同 {@code ServerPlayerGameMode#destroyBlock} 先移除、调
 *       {@code Block#destroy}，可收获（{@code canHarvestBlock}）且非创造才 {@code playerDestroy}（统计、饥饿、掉落、
 *       经验，冰化水）；石头、矿石等需工具者碎而不掉。猪灵守护方块整击激怒一次。</li>
 *   <li>⚠ 不调 {@code playerWillDestroy}：它每块向旁人发破坏粒子与音效，256 块时太吵；改为每 {@link #PARTICLE_EVERY}
 *       块发一组方块粒子，整击只在最近一块处播一次破坏音（参考维特卢姆[Viltrumite]），其余职责在此补齐。</li>
 *   <li>⚠ 双格方块（门、高草、高花）任一半先破，另一半随形状更新连带消失并按自身战利品表掉落：生存恰掉一份；创造
 *       或不可收获时破上半，同原版先以 {@code UPDATE_SUPPRESS_DROPS} 抹掉下半，否则下半会掉。邻居形状更新会清掉
 *       该标志，只标在本格上无效。</li>
 *   <li>⚠ 点数门槛、硬度档、块数上限、粒子与音效均为占位。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AreaImpact
 * @see CrashBreach
 * @since 1.0.0
 */

public final class AreaImpactShatter {

    private AreaImpactShatter() {}

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
            if (AreaImpact.isInCone(origin, unit, radius, pos.getCenter())) cells.add(pos.immutable());
        }
        return byDistance(cells, origin);
    }

    public static @NotNull List<BlockPos> getFanCells(@NotNull Vec3 feet, double height, @NotNull Vec3 axis,
                                                      double radius, double halfAngleDegrees, @NotNull Vec3 from) {
        if (axis.lengthSqr() < MIN_LENGTH_SQR || !(radius > 0.0D)) return List.of();

        int bottom = Mth.floor(feet.y + 0.5D);
        int top = Mth.floor(feet.y + height) + 1;
        double minCos = Math.cos(Math.toRadians(halfAngleDegrees)) - ANGLE_SLACK;
        List<BlockPos> cells = new ArrayList<>();
        for (int x = Mth.floor(feet.x - radius); x <= Mth.floor(feet.x + radius); x++) {
            for (int z = Mth.floor(feet.z - radius); z <= Mth.floor(feet.z + radius); z++) {
                double dx = x + 0.5D - feet.x;
                double dz = z + 0.5D - feet.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < MIN_REACH || distance > radius) continue;
                if ((dx * axis.x + dz * axis.z) / distance < minCos) continue;
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
        float maxHardness = getMaxHardness(points);
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
        if (first != null) {
            level.playSound(null, first, sound.getBreakSound(), SoundSource.BLOCKS, SOUND_VOLUME, SOUND_PITCH);
        }
        if (guarded) PiglinAi.angerNearbyPiglins(player, false);
        return broken;
    }

    private static boolean isExposed(ServerPlayer player, ServerLevel level, Vec3 from, BlockPos pos) {
        BlockHitResult hit = level.clip(new ClipContext(from, pos.getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private static void breakQuietly(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state,
                                     boolean showParticles) {
        if (showParticles) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D,
                    pos.getY() + 0.5D, pos.getZ() + 0.5D, PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD,
                    PARTICLE_SPREAD, PARTICLE_SPEED);
        }
        boolean harvest = !player.isCreative() && state.canHarvestBlock(level, pos, player);
        if (!harvest) clearLowerHalf(level, pos, state);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_ALL);
        state.getBlock().destroy(level, pos, state);
        if (harvest) state.getBlock().playerDestroy(level, player, pos, state, null, ItemStack.EMPTY);
    }

    private static void clearLowerHalf(ServerLevel level, BlockPos pos, BlockState state) {
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
