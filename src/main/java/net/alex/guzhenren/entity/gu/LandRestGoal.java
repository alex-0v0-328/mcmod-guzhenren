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
 * 歇息型飞行蛊共用的降落与地面歇息 AI 目标。
 * <ul>
 *   <li>落点：正下方首块地面，至多 64 格，原版寻路判可行走才取，水、岩浆、伤害方块不给 1 点生命的蛊歇脚。</li>
 *   <li>{@link #tryRetargetGround} 从蛊自身下扫，不读高度图（列顶可能是头顶的树冠、悬垂、屋顶，从下够不着）；遇非
 *       {@link #isOpen}（{@code MOTION_BLOCKING} 判断取反：无可站立物且无流体）地面即停，只取步行生物能站处。</li>
 *   <li>寻路至 1.5 格内后直接驱动：{@link #tickLanding} 瞄准表面略下方，以碰撞收尾，不下落的蛊靠此置 {@code onGround}；
 *       着地才歇 160～200 tick（清空运动、偶尔转向）。</li>
 *   <li>无物下拉，脚下失去承托（破坏或变流体）即起飞；停在落点前的降落至多每 {@link #RETARGET_INTERVAL_TICKS} tick 重
 *       扫；{@link #canUse} 让歇息中加载的蛊（无降落请求）直接重歇一轮、不重播降落。</li>
 *   <li>地面不安全、降落停滞、寻路失败经 {@link RestingFlyingGuEntity#takeOff()} 中止；威胁打断时 {@link #stop} 也起飞
 *       （逃跑路径未启动亦然），绝不卡在非飞行阶段。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
