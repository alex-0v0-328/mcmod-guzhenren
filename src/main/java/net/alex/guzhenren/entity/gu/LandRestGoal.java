package net.alex.guzhenren.entity.gu;

import java.util.EnumSet;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * 歇息型飞行蛊（{@link RestingFlyingGuEntity}）共用的降落与地面歇息 AI 目标。
 * <ul>
 *   <li>落点选择正下方至多 64 格内的首块地面，且须经原版 {@link WalkNodeEvaluator} 判定为 {@link PathType#WALKABLE}，避
 *       免只有 1 点生命的野生蛊落在水面、岩浆或伤害方块上。</li>
 *   <li>{@link #tryRetargetGround} 从蛊虫当前坐标向下逐格扫描而非读取高度图（高度图列顶可能是头顶树冠、悬垂或屋顶，从下
 *       方无法到达），遇到首个非 {@link #isOpen}（即阻挡移动或含流体）的方块即停止，只取上方空间通畅的可站立表面。</li>
 *   <li>寻路飞至落点 1.5 格内后转为直接驱动速度：{@link #tickLanding} 指向方块表面略下方并靠碰撞触地，为无重力的飞行蛊
 *       触发 {@code onGround}；触地后进入 160～200 tick 歇息（清零速度并偶发随机转头）。</li>
 *   <li>因实体无重力，歇息期间若脚下承托方块被破坏或变为流体则立即起飞；降落途中若寻路提前结束，至多每
 *       {@link #RETARGET_INTERVAL_TICKS} tick 重新扫描一次落点；{@link #canUse} 允许从存档恢复为歇息阶段的蛊虫（此时无
 *       降落请求标志）直接续上一轮歇息，无需重播降落。</li>
 *   <li>地面不安全、降落超时、寻路失败时均通过 {@link RestingFlyingGuEntity#takeOff()} 中止并恢复飞行；被玩家威胁打断时
 *       {@link #stop} 也会强制起飞（即使逃跑路径尚未建好），保证绝不卡在非飞行阶段。</li>
 * </ul>
 */

public class LandRestGoal extends Goal {

    private static final double LAND_SPEED_MODIFIER = 1.0D;
    private static final double ARRIVAL_RANGE = 1.5D;
    private static final double TOUCHDOWN_SPEED = 0.1D;
    private static final double TOUCHDOWN_DEPTH = 0.5D;
    private static final double GROUND_PROBE_DEPTH = 0.2D;
    private static final int GROUND_SCAN_DEPTH = 64;
    private static final int REST_TICKS = 8 * Ticks.SECOND;
    private static final int REST_JITTER_TICKS = 41;
    private static final int LOOK_AROUND_ROLL = 80;
    private static final int LOOK_YAW_SPREAD = 181;
    private static final int LOOK_YAW_CENTER = 90;
    private static final int LANDING_TIMEOUT_TICKS = 30 * Ticks.SECOND;
    private static final int RETARGET_INTERVAL_TICKS = 10;
    private final RestingFlyingGuEntity gu;
    private Vec3 landingSpot = Vec3.ZERO;
    private int restRemaining;
    private int landingTicks;
    private int retargetTicks;

    public LandRestGoal(RestingFlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return (gu.isWantingToLand() || gu.getPhase() == RestingFlyingGuEntity.FlightPhase.RESTING)
                && FleePlayerGoal.findNearestThreat(gu) == null;
    }

    @Override
    public boolean canContinueToUse() {
        return gu.getPhase() != RestingFlyingGuEntity.FlightPhase.FLYING
                && FleePlayerGoal.findNearestThreat(gu) == null;
    }

    @Override
    public void start() {
        restRemaining = 0;
        landingTicks = 0;
        if (gu.getPhase() == RestingFlyingGuEntity.FlightPhase.RESTING) {
            restRemaining = restTicks();
            return;
        }
        gu.beginLanding();
        if (!tryRetargetGround()) gu.takeOff();
    }

    @Override
    public void stop() {
        restRemaining = 0;
        gu.getNavigation().stop();
        if (gu.getPhase() != RestingFlyingGuEntity.FlightPhase.FLYING) gu.takeOff();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (gu.getPhase() == RestingFlyingGuEntity.FlightPhase.LANDING) tickLanding();
        else tickResting();
    }

    private void tickLanding() {
        landingTicks++;
        if (retargetTicks > 0) retargetTicks--;
        if (gu.onGround()) {
            gu.getNavigation().stop();
            gu.setDeltaMovement(Vec3.ZERO);
            gu.beginResting();
            restRemaining = restTicks();
        } else if (landingTicks >= LANDING_TIMEOUT_TICKS) {
            gu.takeOff();
        } else if (gu.position().distanceTo(landingSpot) < ARRIVAL_RANGE) {
            gu.getNavigation().stop();
            Vec3 drive = landingSpot.subtract(0.0D, TOUCHDOWN_DEPTH, 0.0D).subtract(gu.position());
            gu.setDeltaMovement(drive.normalize().scale(TOUCHDOWN_SPEED));
        } else if (gu.getNavigation().isDone() && retargetTicks <= 0 && !tryRetargetGround()) {
            gu.takeOff();
        }
    }

    private void tickResting() {
        gu.setDeltaMovement(Vec3.ZERO);
        if (--restRemaining <= 0 || hasLostGround()) {
            gu.takeOff();
            return;
        }
        if (gu.getRandom().nextInt(LOOK_AROUND_ROLL) == 0) {
            float yaw = gu.getYRot() + gu.getRandom().nextInt(LOOK_YAW_SPREAD) - LOOK_YAW_CENTER;
            gu.setYRot(yaw);
            gu.setYBodyRot(yaw);
            gu.setYHeadRot(yaw);
        }
    }

    private boolean hasLostGround() {
        return isOpen(BlockPos.containing(gu.position().subtract(0.0D, GROUND_PROBE_DEPTH, 0.0D)));
    }

    private int restTicks() {
        return REST_TICKS + gu.getRandom().nextInt(REST_JITTER_TICKS);
    }

    private boolean tryRetargetGround() {
        retargetTicks = RETARGET_INTERVAL_TICKS;
        BlockPos.MutableBlockPos surface = gu.blockPosition().mutable();
        int lowest = Math.max(gu.level().getMinBuildHeight(), surface.getY() - GROUND_SCAN_DEPTH);
        while (surface.getY() > lowest && isOpen(surface.below())) surface.move(Direction.DOWN);
        if (isOpen(surface.below())) return false;
        landingSpot = Vec3.atBottomCenterOf(surface);
        return WalkNodeEvaluator.getPathTypeStatic(gu, surface) == PathType.WALKABLE
                && gu.getNavigation().moveTo(landingSpot.x, landingSpot.y, landingSpot.z, LAND_SPEED_MODIFIER);
    }

    @SuppressWarnings("deprecation")
    private boolean isOpen(BlockPos pos) {
        BlockState state = gu.level().getBlockState(pos);
        return !state.blocksMotion() && state.getFluidState().isEmpty();
    }
}
