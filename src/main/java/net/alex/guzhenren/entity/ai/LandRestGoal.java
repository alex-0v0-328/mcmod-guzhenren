package net.alex.guzhenren.entity.ai;

import java.util.EnumSet;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.entity.RestingFlyingGuEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * The landing and ground rest goal shared by the resting flying Gu entities.
 *
 * <p>A requested landing finds the first ground below the entity, at most 64 blocks down, and takes it only
 * when vanilla pathfinding classifies it as walkable, so water, lava and damaging blocks never become a resting
 * place for a one-health Gu. {@link #retargetGround} scans down from the Gu itself rather than reading the
 * column heightmap: that top can be a canopy, an overhang or a roof above the Gu, which it could never settle
 * on from below, and the scan stops as soon as it finds ground that is not {@link #isOpen} -- the
 * {@code MOTION_BLOCKING} heightmap's own test, inverted: nothing to stand on and no fluid -- so only ground a
 * walking mob would stand on is ever offered as a landing spot. Navigation brings the entity within 1.5 blocks
 * of the spot and a direct drive finishes the descent: {@link #tickLanding} settles for the spot rather than
 * on it, aiming just below the surface so the drive ends in a collision, which is what sets {@code onGround}
 * for a Gu that never falls; only touchdown starts the 160 to 200 tick rest with cleared motion and occasional
 * heading changes. {@link #canUse} lets the goal pick a rest back up directly for a Gu loaded mid-rest, which
 * carries no landing request, so it resumes a fresh rest without replaying its landing. Unsafe ground, a
 * stalled landing or a failed navigation target aborts through {@link RestingFlyingGuEntity#takeOff()} --
 * {@link #stop} also takes off whenever a threat interrupts rest, even when its escape path cannot start --
 * so the goal cannot wedge the entity in a non-flying phase.
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
    private static final int GROUND_SCAN_DEPTH = 64;
    private static final int REST_TICKS = 8 * Ticks.SECOND;
    private static final int REST_JITTER_TICKS = 41;
    private static final int LOOK_AROUND_ROLL = 80;
    private static final int LOOK_YAW_SPREAD = 181;
    private static final int LOOK_YAW_CENTER = 90;
    private static final int LANDING_TIMEOUT_TICKS = 30 * Ticks.SECOND;
    private final RestingFlyingGuEntity gu;
    private Vec3 landingSpot = Vec3.ZERO;
    private int restRemaining;
    private int landingTicks;

    public LandRestGoal(RestingFlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return (gu.wantsToLand() || gu.phase() == RestingFlyingGuEntity.FlightPhase.RESTING)
                && FleePlayerGoal.nearestThreat(gu) == null;
    }

    @Override
    public boolean canContinueToUse() {
        return gu.phase() != RestingFlyingGuEntity.FlightPhase.FLYING
                && FleePlayerGoal.nearestThreat(gu) == null;
    }

    @Override
    public void start() {
        restRemaining = 0;
        landingTicks = 0;
        if (gu.phase() == RestingFlyingGuEntity.FlightPhase.RESTING) {
            restRemaining = restTicks();
            return;
        }
        gu.beginLanding();
        if (!retargetGround()) gu.takeOff();
    }

    @Override
    public void stop() {
        restRemaining = 0;
        gu.getNavigation().stop();
        if (gu.phase() != RestingFlyingGuEntity.FlightPhase.FLYING) gu.takeOff();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (gu.phase() == RestingFlyingGuEntity.FlightPhase.LANDING) tickLanding();
        else tickResting();
    }

    private void tickLanding() {
        landingTicks++;
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
        } else if (gu.getNavigation().isDone() && !retargetGround()) {
            gu.takeOff();
        }
    }

    private void tickResting() {
        gu.setDeltaMovement(Vec3.ZERO);
        if (--restRemaining <= 0) {
            gu.takeOff();
            return;
        }
        if (gu.getRandom().nextInt(LOOK_AROUND_ROLL) == 0) {
            gu.setYRot(gu.getYRot() + gu.getRandom().nextInt(LOOK_YAW_SPREAD) - LOOK_YAW_CENTER);
        }
    }

    private int restTicks() {
        return REST_TICKS + gu.getRandom().nextInt(REST_JITTER_TICKS);
    }

    private boolean retargetGround() {
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
